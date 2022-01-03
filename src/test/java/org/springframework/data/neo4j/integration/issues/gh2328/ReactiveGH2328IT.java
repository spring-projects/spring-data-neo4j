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
package org.springframework.data.neo4j.integration.issues.gh2328;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Motörhead - Better Motörhead Than Dead - Live At Hammersmith
 */
@Neo4jIntegrationTest
class ReactiveGH2328IT extends TestBase {

	@Test
	void queriesFromCustomLocationsShouldBeFound(@Autowired SomeRepository someRepository) {

		someRepository.getSomeEntityViaNamedQuery()
				.as(StepVerifier::create)
				.expectNextMatches(this::requirements)
				.verifyComplete();
	}

	public interface SomeRepository extends ReactiveNeo4jRepository<SomeEntity, UUID> {

		// Without a custom query, repository creation would fail with
		// Could not create query for
		// public abstract org.springframework.data.neo4j.integration.issues.gh2328.SomeEntity org.springframework.data.neo4j.integration.issues.gh2328.GH2328IT$SomeRepository.getSomeEntityViaNamedQuery()!
		// Reason: No property getSomeEntityViaNamedQuery found for type SomeEntity!;
		Mono<SomeEntity> getSomeEntityViaNamedQuery();
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true, namedQueriesLocation = "more-custom-queries.properties")
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
