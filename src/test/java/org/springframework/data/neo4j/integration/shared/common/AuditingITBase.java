/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * Shared information for both imperative and reactive auditing tests.
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public abstract class AuditingITBase {
	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static final String EXISTING_THING_NAME = "An old name";
	protected static final String EXISTING_THING_CREATED_BY = "The creator";
	protected static final LocalDateTime EXISTING_THING_CREATED_AT = LocalDateTime.of(2013, 5, 6, 8, 0);
	protected static final LocalDateTime DEFAULT_CREATION_AND_MODIFICATION_DATE = LocalDateTime.of(2018, 7, 1, 8, 0);

	private final Driver driver;

	private final BookmarkCapture bookmarkCapture;

	protected Long idOfExistingThing;
	protected String idOfExistingThingWithGeneratedId = "somethingUnique";

	protected AuditingITBase(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	protected void setupData() {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");

			idOfExistingThing = transaction.run(
					"CREATE (t:ImmutableAuditableThing {name: $name, createdBy: $createdBy, createdAt: $createdAt}) RETURN id(t) as id",
					Values.parameters("name", EXISTING_THING_NAME, "createdBy", EXISTING_THING_CREATED_BY, "createdAt",
							EXISTING_THING_CREATED_AT))
					.single().get("id").asLong();

			transaction.run(
					"CREATE (t:ImmutableAuditableThingWithGeneratedId {name: $name, createdBy: $createdBy, createdAt: $createdAt, id: $id}) RETURN t.id as id",
					Values.parameters("name", EXISTING_THING_NAME, "createdBy", EXISTING_THING_CREATED_BY, "createdAt",
							EXISTING_THING_CREATED_AT, "id", idOfExistingThingWithGeneratedId));

			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	protected void verifyDatabase(long id, ImmutableAuditableThing expectedValues) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Node node = session
					.run("MATCH (t:ImmutableAuditableThing) WHERE id(t) = $id RETURN t", Values.parameters("id", id)).single()
					.get("t").asNode();

			assertDataMatch(expectedValues, node);
		}
	}

	protected void verifyDatabase(String id, ImmutableAuditableThingWithGeneratedId expectedValues) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Node node = session.run("MATCH (t:ImmutableAuditableThingWithGeneratedId) WHERE t.id = $id RETURN t",
					Values.parameters("id", id)).single().get("t").asNode();

			assertDataMatch(expectedValues, node);
		}
	}

	private void assertDataMatch(AuditableThing expectedValues, Node node) {
		assertThat(node.get("name").asString()).isEqualTo(expectedValues.getName());
		assertThat(node.get("createdAt").asLocalDateTime()).isEqualTo(expectedValues.getCreatedAt());
		assertThat(node.get("createdBy").asString()).isEqualTo(expectedValues.getCreatedBy());

		Value modifiedAt = node.get("modifiedAt");
		Value modifiedBy = node.get("modifiedBy");

		if (expectedValues.getModifiedAt() == null) {
			assertThat(modifiedAt.isNull()).isTrue();
		} else {
			assertThat(modifiedAt.asLocalDateTime()).isEqualTo(expectedValues.getModifiedAt());
		}

		if (expectedValues.getModifiedBy() == null) {
			assertThat(modifiedBy.isNull()).isTrue();
		} else {
			assertThat(modifiedBy.asString()).isEqualTo(expectedValues.getModifiedBy());
		}
	}
}
