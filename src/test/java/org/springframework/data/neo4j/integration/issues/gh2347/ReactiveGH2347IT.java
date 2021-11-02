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
package org.springframework.data.neo4j.integration.issues.gh2347;

import reactor.test.StepVerifier;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ReactiveGH2347IT extends TestBase {

	@Test
	void entitiesWithAssignedIdsSavedInBatchMustBeIdentifiableWithTheirInternalIds(
			@Autowired ApplicationRepository applicationRepository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture
	) {
		applicationRepository
				.saveAll(Collections.singletonList(createData()))
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

		assertSingleApplicationNodeWithMultipleWorkflows(driver, bookmarkCapture);
	}

	@Test
	void entitiesWithAssignedIdsMustBeIdentifiableWithTheirInternalIds(
			@Autowired ApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		applicationRepository
				.save(createData())
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();
		assertSingleApplicationNodeWithMultipleWorkflows(driver, bookmarkCapture);
	}

	@Test // GH-2346
	void relationshipsStartingAtEntitiesWithAssignedIdsShouldBeCreated(
			@Autowired ApplicationRepository applicationRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		createData((applications, workflows) -> {
			applicationRepository.saveAll(applications)
					.as(StepVerifier::create)
					.expectNextCount(2L)
					.verifyComplete();

			assertMultipleApplicationsNodeWithASingleWorkflow(driver, bookmarkCapture);
		});
	}

	@Test // GH-2346
	void relationshipsStartingAtEntitiesWithAssignedIdsShouldBeCreatedOtherDirection(
			@Autowired WorkflowRepository workflowRepository,
			@Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture
	) {
		createData((applications, workflows) -> {
			workflowRepository.saveAll(workflows)
					.as(StepVerifier::create)
					.expectNextCount(2L)
					.verifyComplete();

			assertMultipleApplicationsNodeWithASingleWorkflow(driver, bookmarkCapture);
		});
	}

	interface ApplicationRepository extends ReactiveNeo4jRepository<Application, String> {
	}

	interface WorkflowRepository extends ReactiveNeo4jRepository<Workflow, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
