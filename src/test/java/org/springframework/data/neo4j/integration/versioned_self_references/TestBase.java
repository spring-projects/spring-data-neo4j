/*
 * Copyright 2011-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.versioned_self_references;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.provider.Arguments;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.ResultStatement;
import org.neo4j.cypherdsl.core.executables.ExecutableStatement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@TestMethodOrder(MethodOrderer.DisplayName.class)
abstract class TestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private static final Supplier<Long> sequenceGenerator = new Supplier<>() {

		private final AtomicLong source = new AtomicLong(0L);

		@Override
		public Long get() {
			return source.incrementAndGet();
		}
	};

	@Autowired
	private Driver driver;

	@Autowired
	BookmarkCapture bookmarkCapture;

	@BeforeAll
	protected static void clearDatabase(@Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n");
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	static Stream<Arguments> typeAndNewInstanceSupplier() {
		return Stream.of(
				Arguments.arguments(VersionedExternalIdWithEquals.class,
						(Supplier<VersionedExternalIdWithEquals>) () -> {
							long id = sequenceGenerator.get();
							return new VersionedExternalIdWithEquals(id, "Instance" + id);
						}),

				Arguments.arguments(VersionedExternalIdWithoutEquals.class,
						(Supplier<VersionedExternalIdWithoutEquals>) () -> {
							long id = sequenceGenerator.get();
							return new VersionedExternalIdWithoutEquals(id, "Instance" + id);
						}),

				Arguments.arguments(VersionedExternalIdListBased.class, (Supplier<VersionedExternalIdListBased>) () -> {
					long id = sequenceGenerator.get();
					return new VersionedExternalIdListBased(id, "Instance" + id);
				}),

				Arguments.arguments(VersionedInternalIdWithEquals.class,
						(Supplier<VersionedInternalIdWithEquals>) () -> new VersionedInternalIdWithEquals(
								"An object " + System.currentTimeMillis())),

				Arguments.arguments(VersionedInternalIdWithoutEquals.class,
						(Supplier<VersionedInternalIdWithoutEquals>) () -> new VersionedInternalIdWithoutEquals(
								"An object " + System.currentTimeMillis())),

				Arguments.arguments(VersionedInternalIdListBased.class,
						(Supplier<VersionedInternalIdListBased>) () -> new VersionedInternalIdListBased(
								"An object " + System.currentTimeMillis()))
		);
	}

	static Stream<Arguments> typesForExistingInstanceSupplier() {

		return Stream.of(VersionedExternalIdWithEquals.class, VersionedExternalIdWithoutEquals.class,
				VersionedExternalIdListBased.class,
				VersionedInternalIdWithEquals.class, VersionedInternalIdWithoutEquals.class,
				VersionedInternalIdListBased.class).map(Arguments::of);
	}

	Long createInstance(Class<?> type) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction tx = session.beginTransaction()) {

			Map<String, Object> properties = new HashMap<>();
			properties.put("name", Cypher.anonParameter("Instance @" + System.currentTimeMillis()));
			properties.put("version", 0L);
			String simpleName = type.getSimpleName();
			boolean isExternal = simpleName.contains("External");
			if (isExternal) {
				properties.put("id", Cypher.anonParameter(sequenceGenerator.get()));
			}
			Node nodeTemplate = Cypher.node(simpleName).withProperties(properties);
			ResultStatement statement;
			if (isExternal) {
				statement = Cypher.create(nodeTemplate).returning(nodeTemplate.property("id")).build();
			} else {
				statement = Cypher.create(nodeTemplate).returning(Functions.id(nodeTemplate)).build();
			}

			long id = ExecutableStatement.makeExecutable(statement).fetchWith(tx).get(0).get(0).asLong();

			tx.commit();
			return id;
		}
	}

	long[] createRelatedInstances(Class<?> type) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction tx = session.beginTransaction()) {

			String simpleName = type.getSimpleName();
			boolean isExternal = simpleName.contains("External");

			Supplier<Map<String, Object>> propertySupplier = () -> {
				Map<String, Object> properties = new HashMap<>();
				properties.put("name", Cypher.anonParameter("Instance @" + System.currentTimeMillis()));
				properties.put("version", 0L);
				if (isExternal) {
					properties.put("id", Cypher.anonParameter(sequenceGenerator.get()));
				}
				return properties;
			};
			Node nodeTemplate = Cypher.node(simpleName);
			Node n1 = nodeTemplate.named("n1").withProperties(propertySupplier.get());
			Node n2 = nodeTemplate.named("n2").withProperties(propertySupplier.get());
			ResultStatement statement;
			if (isExternal) {
				statement = Cypher.create(n1).create(n2)
						.merge(n1.relationshipTo(n2, "RELATED"))
						.merge(n2.relationshipTo(n1, "RELATED"))
						.returning(n1.property("id"), n2.property("id"))
						.build();
			} else {
				statement = Cypher.create(n1).create(n2)
						.merge(n1.relationshipTo(n2, "RELATED"))
						.merge(n2.relationshipTo(n1, "RELATED"))
						.returning(Functions.id(n1), Functions.id(n2))
						.build();
			}

			Record record = ExecutableStatement.makeExecutable(statement).fetchWith(tx).get(0);
			long id1 = record.get(0).asLong();
			long id2 = record.get(1).asLong();

			tx.commit();
			return new long[] { id1, id2 };
		}
	}

	<T extends Relatable<T>> void assertDatabase(Long expectedVersion, Class<T> type, T root) {

		String simpleName = type.getSimpleName();
		if (simpleName.contains("External")) {
			assertExternal(expectedVersion, type, root.getId());
		} else if (simpleName.contains("Internal")) {
			assertInternal(expectedVersion, type, root.getId());
		} else {
			fail("Unsupported type: " + type);
		}
	}

	VersionedExternalIdWithEquals createRing(int ringSize) {
		VersionedExternalIdWithEquals start = new VersionedExternalIdWithEquals(sequenceGenerator.get(), "start");
		VersionedExternalIdWithEquals previous = start;
		for (int i = 0; i < ringSize - 1; ++i) {
			VersionedExternalIdWithEquals next = new VersionedExternalIdWithEquals(sequenceGenerator.get(), Integer.toString(i));
			previous.relate(next);
			previous = next;
		}

		start.relate(previous);
		return start;
	}

	int traverseRing(VersionedExternalIdWithEquals root, Consumer<VersionedExternalIdWithEquals> assertion) {
		int cnt = 0;
		VersionedExternalIdWithEquals next = root;
		do {
			assertion.accept(next);

			VersionedExternalIdWithEquals[] relatedObjects = next.getRelatedObjects()
					.toArray(new VersionedExternalIdWithEquals[0]);
			String nextName = Integer.toString(cnt++);
			if (relatedObjects[0].getName().equals(nextName)) {
				next = relatedObjects[0];
			} else if (relatedObjects[1].getName().equals(nextName)) {
				next = relatedObjects[1];
			} else {
				next = null;
			}
		} while (next != null);
		return cnt;
	}

	interface NameOnly {
		String getName();
	}

	private void assertExternal(Long expectedVersion, Class<?> type, Long id) {

		Node nodeTemplate = Cypher.node(type.getSimpleName());
		Node n1 = nodeTemplate.named("n1");
		Node n2 = nodeTemplate.named("n2");
		ResultStatement statement =
				Cypher.match(n1.relationshipTo(n2, "RELATED"))
						.where(n1.property("id").isEqualTo(Cypher.anonParameter(id))
								.and(n2.relationshipTo(n1, "RELATED")))
						.returning(n1.property("version")).build();

		assertImpl(expectedVersion, statement);
	}

	private void assertInternal(Long expectedVersion, Class<?> type, Long id) {

		Node nodeTemplate = Cypher.node(type.getSimpleName());
		Node n1 = nodeTemplate.named("n1");
		Node n2 = nodeTemplate.named("n2");
		ResultStatement statement =
				Cypher.match(n1.relationshipTo(n2, "RELATED"))
						.where(Functions.id(n1).isEqualTo(Cypher.anonParameter(id))
								.and(n2.relationshipTo(n1, "RELATED")))
						.returning(n1.property("version")).build();

		assertImpl(expectedVersion, statement);
	}

	private void assertImpl(Long expectedVersion, ResultStatement resultStatement) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<Record> result = ExecutableStatement.makeExecutable(resultStatement).fetchWith(session);
			assertThat(result).hasSize(1)
					.first().satisfies(record -> {
						long version = record.get(0).asLong();
						assertThat(version).isEqualTo(expectedVersion);
					});
		}
	}
}
