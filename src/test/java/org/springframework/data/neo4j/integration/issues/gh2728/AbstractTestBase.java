/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2728;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
public abstract class AbstractTestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	private TestEntityWithGeneratedDeprecatedId1Repository generatedDeprecatedIdRepository;

	@Autowired
	private TestEntityWithAssignedId1Repository assignedIdRepository;

	@Test
	public void testGeneratedDeprecatedIds() {
		TestEntityWithGeneratedDeprecatedId2 t2 = new TestEntityWithGeneratedDeprecatedId2(null, "v2");
		TestEntityWithGeneratedDeprecatedId1 t1 = new TestEntityWithGeneratedDeprecatedId1(null, "v1", t2);

		TestEntityWithGeneratedDeprecatedId1 result = generatedDeprecatedIdRepository.save(t1);

		TestEntityWithGeneratedDeprecatedId1 freshRetrieved = generatedDeprecatedIdRepository.findById(result.getId()).get();

		Assertions.assertNotNull(result.getRelatedEntity());
		Assertions.assertNotNull(freshRetrieved.getRelatedEntity());
	}

	/**
	 * This is a test to ensure if the fix for the failing test above will continue to work for
	 * assigned ids. For broader test cases please return false for isCypher5Compatible in (Reactive)RepositoryIT
	 */
	@Test
	public void testAssignedIds() {
		TestEntityWithAssignedId2 t2 = new TestEntityWithAssignedId2("second", "v2");
		TestEntityWithAssignedId1 t1 = new TestEntityWithAssignedId1("first", "v1", t2);

		TestEntityWithAssignedId1 result = assignedIdRepository.save(t1);

		TestEntityWithAssignedId1 freshRetrieved = assignedIdRepository.findById(result.getAssignedId()).get();

		Assertions.assertNotNull(result.getRelatedEntity());
		Assertions.assertNotNull(freshRetrieved.getRelatedEntity());
	}

	interface TestEntityWithGeneratedDeprecatedId1Repository extends Neo4jRepository<TestEntityWithGeneratedDeprecatedId1, Long> {
	}

	interface TestEntityWithAssignedId1Repository extends Neo4jRepository<TestEntityWithAssignedId1, String> {
	}

	abstract static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
		                                                     DatabaseSelectionProvider databaseNameProvider) {
			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
