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

import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.schema.IdGenerator;
import org.neo4j.springframework.data.integration.shared.IdGeneratorsITBase;
import org.neo4j.springframework.data.integration.shared.ThingWithGeneratedId;
import org.neo4j.springframework.data.integration.shared.ThingWithIdGeneratedByBean;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class IdGeneratorsIT extends IdGeneratorsITBase {

	private final ThingsWithGeneratedIds thingsWithGeneratedIds;

	private final ThingsWithBeanGeneratedIds thingsWithBeanGeneratedIds;

	@Autowired IdGeneratorsIT(
		ThingsWithGeneratedIds thingsWithGeneratedIds, ThingsWithBeanGeneratedIds thingsWithBeanGeneratedIds,
		Driver driver) {
		super(driver);
		this.thingsWithGeneratedIds = thingsWithGeneratedIds;
		this.thingsWithBeanGeneratedIds = thingsWithBeanGeneratedIds;
	}

	@Test
	void idGenerationWithNewEntityShouldWork() {

		ThingWithGeneratedId t = new ThingWithGeneratedId("Foobar");
		t.setName("Foobar");
		t = thingsWithGeneratedIds.save(t);
		assertThat(t.getTheId())
			.isNotBlank()
			.matches("thingWithGeneratedId-\\d+");

		verifyDatabase(t.getTheId(), t.getName());
	}

	@Test
	void idGenerationByBeansShouldWorkWork() {

		ThingWithIdGeneratedByBean t = new ThingWithIdGeneratedByBean("Foobar");
		t.setName("Foobar");
		t = thingsWithBeanGeneratedIds.save(t);
		assertThat(t.getTheId()).isEqualTo("ImperativeID.");

		verifyDatabase(t.getTheId(), t.getName());
	}

	@Test
	void idGenerationWithNewEntitiesShouldWork() {

		List<ThingWithGeneratedId> things = IntStream.rangeClosed(1, 10)
			.mapToObj(i -> new ThingWithGeneratedId("name" + i))
			.collect(toList());

		Iterable<ThingWithGeneratedId> savedThings = thingsWithGeneratedIds.saveAll(things);
		assertThat(savedThings)
			.hasSize(things.size())
			.extracting(ThingWithGeneratedId::getTheId)
			.allMatch(s -> s.matches("thingWithGeneratedId-\\d+"));

		Set<String> distinctIds = StreamSupport.stream(savedThings.spliterator(), false)
			.map(ThingWithGeneratedId::getTheId).collect(toSet());

		assertThat(distinctIds).hasSize(things.size());
	}

	@Test
	void shouldNotOverwriteExistingId() {

		ThingWithGeneratedId t = thingsWithGeneratedIds.findById(ID_OF_EXISTING_THING).get();
		t.setName("changed");
		t = thingsWithGeneratedIds.save(t);

		assertThat(t.getTheId())
			.isNotBlank()
			.isEqualTo(ID_OF_EXISTING_THING);

		verifyDatabase(t.getTheId(), t.getName());
	}

	public interface ThingsWithGeneratedIds extends CrudRepository<ThingWithGeneratedId, String> {
	}

	public interface ThingsWithBeanGeneratedIds extends CrudRepository<ThingWithIdGeneratedByBean, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ThingWithGeneratedId.class.getPackage().getName());
		}

		@Bean
		public IdGenerator<String> aFancyIdGenerator() {
			return (label, entity) -> "ImperativeID.";
		}
	}
}
