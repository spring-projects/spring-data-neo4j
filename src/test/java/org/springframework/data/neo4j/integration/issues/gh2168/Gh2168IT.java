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
package org.springframework.data.neo4j.integration.issues.gh2168;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class Gh2168IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
			 Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (:DomainObject{id: 'A'})");
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2168
	void findByIdShouldWork(@Autowired DomainObjectRepository domainObjectRepository) {

		Optional<DomainObject> optionalResult = domainObjectRepository.findById("A");
		assertThat(optionalResult)
				.map(DomainObject::getId)
				.hasValue("A");
	}

	@Test // GH-2168
	void compositePropertyCustomConverterDefaultPrefixShouldWork(
			@Autowired DomainObjectRepository repository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {

		DomainObject domainObject = new DomainObject();
		domainObject.setStoredAsMultipleProperties(new UnrelatedObject(true, 4711L));
		domainObject = repository.save(domainObject);

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Node node = session
					.run("MATCH (n:DomainObject {id: $id}) RETURN n", Collections.singletonMap("id", domainObject.getId()))
					.single().get(0).asNode();
			assertThat(node.get("storedAsMultipleProperties.aBooleanValue").asBoolean()).isTrue();
			assertThat(node.get("storedAsMultipleProperties.aLongValue").asLong()).isEqualTo(4711L);
		}

		domainObject = repository.findById(domainObject.getId()).get();
		assertThat(domainObject.getStoredAsMultipleProperties())
				.satisfies(t -> {
					assertThat(t.isABooleanValue()).isTrue();
					assertThat(t.getALongValue()).isEqualTo(4711L);
				});
	}

	// That test and the underlying mapping cause the original issue to fail, as `@ConvertWith` was missing for non-simple
	// types in the lookup that checked whether something is an association or not
	@Test // GH-2168
	void propertyCustomConverterDefaultPrefixShouldWork(
			@Autowired Neo4jMappingContext ctx,
			@Autowired DomainObjectRepository repository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		Neo4jPersistentEntity<?> entity = ctx.getRequiredPersistentEntity(DomainObject.class);
		assertWriteAndReadConversionForProperty(entity, "storedAsSingleProperty", repository, driver, bookmarkCapture);
	}

	@Test // GH-2430
	void propertyConversionsWithBeansShouldWork(
			@Autowired Neo4jMappingContext ctx,
			@Autowired DomainObjectRepository repository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		Neo4jPersistentEntity<?> entity = ctx.getRequiredPersistentEntity(DomainObject.class);
		assertWriteAndReadConversionForProperty(entity, "storedAsAnotherSingleProperty", repository, driver, bookmarkCapture);
	}

	private void assertWriteAndReadConversionForProperty(
			Neo4jPersistentEntity<?> entity,
			String propertyName,
			DomainObjectRepository repository,
			Driver driver,
			BookmarkCapture bookmarkCapture
	) {
		Neo4jPersistentProperty property = entity.getPersistentProperty(propertyName);
		PersistentPropertyAccessor<DomainObject> propertyAccessor = entity.getPropertyAccessor(new DomainObject());

		propertyAccessor.setProperty(property, new UnrelatedObject(true, 4711L));
		DomainObject domainObject = repository.save(propertyAccessor.getBean());

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Node node = session
					.run("MATCH (n:DomainObject {id: $id}) RETURN n",
							Collections.singletonMap("id", domainObject.getId()))
					.single().get(0).asNode();
			assertThat(node.get(propertyName).asString()).isEqualTo("true;4711");
		}

		domainObject = repository.findById(domainObject.getId()).get();
		UnrelatedObject unrelatedObject = (UnrelatedObject) entity.getPropertyAccessor(domainObject).getProperty(property);
		assertThat(unrelatedObject)
				.satisfies(t -> {
					assertThat(t.isABooleanValue()).isTrue();
					assertThat(t.getALongValue()).isEqualTo(4711L);
				});
	}

	interface DomainObjectRepository extends Neo4jRepository<DomainObject, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Bean
		public UnrelatedObjectPropertyConverterAsBean converterBean() {
			return new UnrelatedObjectPropertyConverterAsBean();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
