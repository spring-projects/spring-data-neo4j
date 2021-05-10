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
package org.springframework.data.neo4j.integration.properties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - Metallica
 */
@Neo4jIntegrationTest
class PropertyIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Autowired
	private Driver driver;
	@Autowired
	private Neo4jTemplate template;
	@Autowired
	private BookmarkCapture bookmarkCapture;

	@Test // GH-2118
	void assignedIdNoVersionShouldNotOverwriteUnknownProperties() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run(
					"CREATE (m:SimplePropertyContainer {id: 'id1', knownProperty: 'A', unknownProperty: 'Mr. X'}) RETURN id(m)")
					.consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		updateKnownAndAssertUnknownProperty(DomainClasses.SimplePropertyContainer.class, "id1");
	}

	@Test // GH-2118
	void assignedIdWithVersionShouldNotOverwriteUnknownProperties() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run(
					"CREATE (m:SimplePropertyContainerWithVersion {id: 'id1', version: 1, knownProperty: 'A', unknownProperty: 'Mr. X'}) RETURN id(m)")
					.consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		updateKnownAndAssertUnknownProperty(DomainClasses.SimplePropertyContainerWithVersion.class, "id1");
	}

	@Test // GH-2118
	void generatedIdNoVersionShouldNotOverwriteUnknownProperties() {

		Long id;
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			id = session
					.run("CREATE (m:SimpleGeneratedIDPropertyContainer {knownProperty: 'A', unknownProperty: 'Mr. X'}) RETURN id(m)")
					.single().get(0).asLong();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		updateKnownAndAssertUnknownProperty(DomainClasses.SimpleGeneratedIDPropertyContainer.class, id);
	}

	@Test // GH-2118
	void generatedIdWithVersionShouldNotOverwriteUnknownProperties() {

		Long id;
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			id = session
					.run("CREATE (m:SimpleGeneratedIDPropertyContainerWithVersion {version: 1, knownProperty: 'A', unknownProperty: 'Mr. X'}) RETURN id(m)")
					.single().get(0).asLong();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		updateKnownAndAssertUnknownProperty(DomainClasses.SimpleGeneratedIDPropertyContainerWithVersion.class, id);
	}

	private void updateKnownAndAssertUnknownProperty(Class<? extends DomainClasses.BaseClass> type, Object id) {

		Optional<? extends DomainClasses.BaseClass> optionalContainer = template.findById(id, type);
		assertThat(optionalContainer).isPresent();
		optionalContainer.ifPresent(m -> {
			m.setKnownProperty("A2");
			template.save(m);
		});

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session
					.run("MATCH (m:" + type.getSimpleName() + ") WHERE " + (id instanceof Long ? "id(m) " : "m.id")
						 + " = $id AND m.knownProperty = 'A2' AND m.unknownProperty = 'Mr. X' RETURN count(m)",
							Collections.singletonMap("id", id)).single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test // GH-2118
	void multipleAssignedIdNoVersionShouldNotOverwriteUnknownProperties() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run(
					"CREATE (m:SimplePropertyContainer {id: 'a', knownProperty: 'A', unknownProperty: 'Fix'})  RETURN id(m)")
					.consume();
			session.run(
					"CREATE (m:SimplePropertyContainer {id: 'b', knownProperty: 'B', unknownProperty: 'Foxy'}) RETURN id(m)")
					.consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		DomainClasses.SimplePropertyContainer optionalContainerA = template
				.findById("a", DomainClasses.SimplePropertyContainer.class).get();
		DomainClasses.SimplePropertyContainer optionalContainerB = template
				.findById("b", DomainClasses.SimplePropertyContainer.class).get();
		optionalContainerA.setKnownProperty("A2");
		optionalContainerB.setKnownProperty("B2");

		template.saveAll(Arrays.asList(optionalContainerA, optionalContainerB));

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session
					.run("MATCH (m:SimplePropertyContainer) WHERE m.id in $ids AND m.unknownProperty IS NOT NULL RETURN count(m)",
							Collections.singletonMap("ids", Arrays.asList("a", "b"))).single().get(0).asLong();
			assertThat(cnt).isEqualTo(2L);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2118
	void relationshipPropertiesMustNotBeOverwritten() {

		Long id;
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			id = session
					.run("CREATE (a:IrrelevantSourceContainer) - [:RELATIONSHIP_PROPERTY_CONTAINER {knownProperty: 'A', unknownProperty: 'Mr. X'}] -> (:IrrelevantTargetContainer) RETURN id(a)")
					.single().get(0).asLong();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		Optional<DomainClasses.IrrelevantSourceContainer> optionalContainer = template
				.findById(id, DomainClasses.IrrelevantSourceContainer.class);
		assertThat(optionalContainer).hasValueSatisfying(c -> {
			assertThat(c.getRelationshipPropertyContainer()).isNotNull();
			assertThat(c.getRelationshipPropertyContainer().getId()).isNotNull();
		});

		optionalContainer.ifPresent(c -> {
			c.getRelationshipPropertyContainer().setKnownProperty("A2");
			template.save(c);
		});

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session
					.run("MATCH (m) - [r:RELATIONSHIP_PROPERTY_CONTAINER] -> (:IrrelevantTargetContainer) WHERE id(m) = $id AND r.knownProperty = 'A2' AND r.unknownProperty = 'Mr. X' RETURN count(m)",
							Collections.singletonMap("id", id)).single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test // GH-2118
	void relationshipIdsShouldBeFilled() {

		DomainClasses.RelationshipPropertyContainer rel = new DomainClasses.RelationshipPropertyContainer();
		rel.setKnownProperty("A");
		rel.setIrrelevantTargetContainer(new DomainClasses.IrrelevantTargetContainer());
		DomainClasses.IrrelevantSourceContainer s = template.save(new DomainClasses.IrrelevantSourceContainer(rel));

		assertThat(s.getRelationshipPropertyContainer().getId()).isNotNull();
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session
					.run("MATCH (m) - [r:RELATIONSHIP_PROPERTY_CONTAINER] -> (:IrrelevantTargetContainer) WHERE id(m) = $id AND r.knownProperty = 'A' RETURN count(m)",
							Collections.singletonMap("id", s.getId())).single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test // GH-2118
	void relationshipIdsShouldBeFilledDynamicRelationships() {

		DomainClasses.DynRelSourc1 source = new DomainClasses.DynRelSourc1();
		DomainClasses.RelationshipPropertyContainer rel = new DomainClasses.RelationshipPropertyContainer();
		rel.setKnownProperty("A");
		rel.setIrrelevantTargetContainer(new DomainClasses.IrrelevantTargetContainer());
		source.rels.put("DYN_REL", Collections.singletonList(rel));
		source = template.save(source);
		assertThat(source.getRels().get("DYN_REL").get(0).getId()).isNotNull();

		DomainClasses.DynRelSourc2 source2 = new DomainClasses.DynRelSourc2();
		rel = new DomainClasses.RelationshipPropertyContainer();
		rel.setKnownProperty("A");
		rel.setIrrelevantTargetContainer(new DomainClasses.IrrelevantTargetContainer());
		source2.rels.put("DYN_REL", rel);
		source2 = template.save(source2);
		assertThat(source2.getRels().get("DYN_REL").getId()).isNotNull();
	}

	@Test // GH-2123
	void customConvertersForRelsMustBeTakenIntoAccount() {

		Date now = new Date();
		DomainClasses.WeirdSource source = new DomainClasses.WeirdSource(now, new DomainClasses.IrrelevantTargetContainer());
		template.save(source).getMyFineId();
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session
					.run("MATCH (m) - [r:ITS_COMPLICATED] -> (n) WHERE m.id = $id RETURN count(m)",
							Collections.singletonMap("id", now.getTime())).single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test // GH-2124
	void shouldNotFailWithEmptyOrNullRelationshipProperties() {

		DomainClasses.LonelySourceContainer s = template.save(new DomainClasses.LonelySourceContainer());
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session.run("MATCH (m) WHERE id(m) = $id RETURN count(m)", Collections.singletonMap("id", s.getId())).single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
