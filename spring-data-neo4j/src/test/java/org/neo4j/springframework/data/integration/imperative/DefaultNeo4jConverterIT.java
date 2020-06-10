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
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Property;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Davide Fantuzzi
 * @author Andrea Santurbano
 */
@Neo4jIntegrationTest
class DefaultNeo4jConverterIT {

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	@Test
	void itShouldReturnsAllTheRelatedEntities(@Autowired Entity2Repository entity2Repository) {
		Entity1 firstEntity1 = new Entity1("1-2-3");
		Entity1 secondEntity1 = new Entity1("4-5-6");
		Set<Entity1> entity1List = new HashSet<>(Arrays.asList(firstEntity1, secondEntity1));
		Entity2 entity2 = new Entity2("7-8-9", entity1List);

		entity2Repository.save(entity2);
		Optional<Entity2> optionalEntity2 = entity2Repository.findById(entity2.id);

		assertThat(optionalEntity2).isPresent();
		assertThat(optionalEntity2).map(e -> e.entity1List).contains(entity1List);
	}

	@Node
	private static class Entity1 {
		@Id
		@Property("my_internal_id")
		private final String id;

		Entity1(String id) {
			this.id = id;
		}

		@Override public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Entity1 entity1 = (Entity1) o;
			return Objects.equals(id, entity1.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}

		@Override public String toString() {
			return "Entity1{" +
				"id='" + id + '\'' +
				'}';
		}
	}

	@Node
	private static class Entity2 {
		@Id
		private final String id;

		@Relationship(value = "REL", direction = Relationship.Direction.INCOMING)
		private final Set<Entity1> entity1List;

		Entity2(String id, Set<Entity1> entity1List) {
			this.id = id;
			this.entity1List = entity1List;
		}

		@Override public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Entity2 entity2 = (Entity2) o;
			return Objects.equals(id, entity2.id) &&
				Objects.equals(entity1List, entity2.entity1List);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, entity1List);
		}

		@Override public String toString() {
			return "Entity2{" +
				"id='" + id + '\'' +
				", entity1List=" + entity1List +
				'}';
		}
	}

	@Repository
	interface Entity2Repository extends Neo4jRepository<Entity2, String> {
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

	}
}
