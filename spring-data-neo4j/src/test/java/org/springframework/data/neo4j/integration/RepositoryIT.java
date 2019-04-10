/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RepositoryIT.Config.class)
@Testcontainers
class RepositoryIT {

	@Container
	private static Neo4jContainer neo4jContainer = new Neo4jContainer().withoutAuthentication();

	private final PersonRepository repository;

	RepositoryIT(@Autowired PersonRepository repository) {
		this.repository = repository;
	}

	@Test
	void repositoryCallFailsBecauseOfUnsupportedOperationException() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll());
	}

	@Test
	void callCustomCypher() {
		List<Map<String, Object>> actual = repository.customQuery();
		assertThat(actual)
			.isNotNull()
			.isNotEmpty()
			.first().extracting(record -> record.get("1"))
			.isEqualTo(1L);
	}

	@Test
	void findBySimpleProperty() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findByName());
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config {

		@Bean
		public Driver driver() {

			String boltUrl = neo4jContainer.getBoltUrl();
			return GraphDatabase.driver(boltUrl, AuthTokens.none());
		}

		@Bean
		public NodeManagerFactory nodeManagerFactory(Driver driver) {

			return new NodeManagerFactory(driver, Person.class);
		}

		@Bean
		public PlatformTransactionManager transactionManager(Driver driver) {

			return new Neo4jTransactionManager(driver);
		}
	}
}
