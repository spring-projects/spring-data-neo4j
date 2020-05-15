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
package org.neo4j.springframework.data.integration.imperative;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.integration.imperative.repositories.ThingRepository;
import org.neo4j.springframework.data.integration.shared.CallbacksITBase;
import org.neo4j.springframework.data.integration.shared.ThingWithAssignedId;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.repository.event.BeforeBindCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class CallbacksIT extends CallbacksITBase {


	@Autowired CallbacksIT(Driver driver) {
		super(driver);
	}

	@Test
	void onBeforeBindShouldBeCalledForSingleEntity(@Autowired ThingRepository repository) {

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
		thing.setName("A name");
		thing = repository.save(thing);

		assertThat(thing.getName()).isEqualTo("A name (Edited)");

		verifyDatabase(Collections.singletonList(thing));
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntities(@Autowired ThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1");
		thing1.setName("A name");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2");
		thing2.setName("Another name");
		Iterable<ThingWithAssignedId> savedThings = repository.saveAll(Arrays.asList(thing1, thing2));

		assertThat(savedThings).extracting(ThingWithAssignedId::getName)
			.containsExactlyInAnyOrder("A name (Edited)", "Another name (Edited)");

		verifyDatabase(savedThings);
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		BeforeBindCallback<ThingWithAssignedId> nameChanger() {
			return entity -> {
				ThingWithAssignedId updatedThing = new ThingWithAssignedId(entity.getTheId());
				updatedThing.setName(entity.getName() + " (Edited)");
				return updatedThing;
			};
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

	}
}
