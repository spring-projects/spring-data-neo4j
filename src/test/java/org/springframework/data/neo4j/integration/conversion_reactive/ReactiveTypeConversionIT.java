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
package org.springframework.data.neo4j.integration.conversion_reactive;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
import org.springframework.data.neo4j.integration.shared.common.ThingWithUUIDID;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Tom Morello - The Atlas Underground
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveTypeConversionIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Test
	void idsShouldBeConverted(@Autowired ConvertedIDsRepository repository) {

		List<ThingWithUUIDID> stored = new ArrayList<>();
		StepVerifier.create(repository.save(new ThingWithUUIDID("a thing"))).recordWith(() -> stored).expectNextCount(1L)
				.verifyComplete();

		StepVerifier.create(repository.findById(stored.get(0).getId())).expectNextCount(1L).verifyComplete();
	}

	@Test
	void relatedIdsShouldBeConverted(@Autowired ConvertedIDsRepository repository) {

		ThingWithUUIDID aThing = new ThingWithUUIDID("a thing");
		aThing.setAnotherThing(new ThingWithUUIDID("Another thing"));

		List<ThingWithUUIDID> stored = new ArrayList<>();
		StepVerifier.create(repository.save(aThing)).recordWith(() -> stored).expectNextCount(1L).verifyComplete();

		ThingWithUUIDID savedThing = stored.get(0);
		assertThat(savedThing.getId()).isNotNull();
		assertThat(savedThing.getAnotherThing().getId()).isNotNull();

		StepVerifier.create(
				Flux.concat(repository.findById(savedThing.getId()), repository.findById(savedThing.getAnotherThing().getId())))
				.expectNextCount(2L).verifyComplete();
	}

	public interface ConvertedIDsRepository extends ReactiveNeo4jRepository<ThingWithUUIDID, UUID> {}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jConversions neo4jConversions() {
			return new Neo4jConversions(Collections.singleton(new ThingWithCustomTypes.CustomTypeConverter()));
		}
	}
}
