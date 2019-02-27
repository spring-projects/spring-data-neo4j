/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ContextConfiguration
@ExtendWith(SpringExtension.class)
@Testcontainers
class RepositoryCreationTest {

	@Container private static Neo4jContainer neo4jContainer = new Neo4jContainer().withAdminPassword(null);

	@Autowired private PersonRepository repository;

	@Test
	void repositoryGetsCreated() {
		assertThat(repository).isNotNull();
	}

	@Test
	void repositoryCallFailsBecauseOfUnsupportedOperationException() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll());
	}

	@Test
	void callCustomCypher() {
		List<Record> actual = repository.customQuery();
		assertThat(actual).isNotNull();
	}

	@Configuration
	@EnableNeo4jRepositories
	static class Config {

		@Bean
		public Neo4jOperations neo4jTemplate() {
			String boltUrl = neo4jContainer.getBoltUrl();
			Driver driver = GraphDatabase.driver(boltUrl, AuthTokens.none());

			return new Neo4jTemplate(driver);
		}

	}
}
