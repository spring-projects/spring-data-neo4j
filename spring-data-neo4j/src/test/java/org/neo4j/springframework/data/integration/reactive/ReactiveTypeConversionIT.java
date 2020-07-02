/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.reactive;

import static org.assertj.core.api.Assertions.*;
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.core.convert.Neo4jConversions;
import org.neo4j.springframework.data.integration.shared.ThingWithCustomTypes;
import org.neo4j.springframework.data.integration.shared.ThingWithUUIDID;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Tom Morello - The Atlas Underground
 */
@ExtendWith(Neo4jExtension.class)
@SpringJUnitConfig
@DirtiesContext
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveTypeConversionIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	@Autowired ReactiveTypeConversionIT(Driver driver) {
		this.driver = driver;
	}

	@Test
	void idsShouldBeConverted(@Autowired ConvertedIDsRepository repository) {

		List<ThingWithUUIDID> stored = new ArrayList<>();
		StepVerifier.create(repository.save(new ThingWithUUIDID("a thing")))
			.recordWith(() -> stored)
			.expectNextCount(1L)
			.verifyComplete();

		StepVerifier.create(repository.findById(stored.get(0).getId()))
			.expectNextCount(1L)
			.verifyComplete();
	}

	@Test
	void relatedIdsShouldBeConverted(@Autowired ConvertedIDsRepository repository) {

		ThingWithUUIDID aThing = new ThingWithUUIDID("a thing");
		aThing.setAnotherThing(new ThingWithUUIDID("Another thing"));

		List<ThingWithUUIDID> stored = new ArrayList<>();
		StepVerifier.create(repository.save(aThing))
			.recordWith(() -> stored)
			.expectNextCount(1L)
			.verifyComplete();

		ThingWithUUIDID savedThing = stored.get(0);
		assertThat(savedThing.getId()).isNotNull();
		assertThat(savedThing.getAnotherThing().getId()).isNotNull();

		StepVerifier.create(Flux.concat(repository.findById(savedThing.getId()),
			repository.findById(savedThing.getAnotherThing().getId())))
			.expectNextCount(2L)
			.verifyComplete();
	}

	public interface ConvertedIDsRepository extends ReactiveNeo4jRepository<ThingWithUUIDID, UUID> {
	}

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
