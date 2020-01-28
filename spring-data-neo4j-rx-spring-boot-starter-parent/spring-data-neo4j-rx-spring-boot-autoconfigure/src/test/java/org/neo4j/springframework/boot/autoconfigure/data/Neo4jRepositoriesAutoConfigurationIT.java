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
package org.neo4j.springframework.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.neo4j.springframework.boot.autoconfigure.data.bikes.BikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * @author Michael J. Simons
 */
@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = Neo4jRepositoriesAutoConfigurationIT.Neo4jContainerBasedTestPropertyProvider.class)
public class Neo4jRepositoriesAutoConfigurationIT {

	@Container private static Neo4jContainer neo4jServer = new Neo4jContainer<>();

	private final BikeRepository bikeRepository;

	@Autowired
	Neo4jRepositoriesAutoConfigurationIT(BikeRepository bikeRepository) {
		this.bikeRepository = bikeRepository;
	}

	@Test
	void ensureRepositoryIsReady() {

		assertThat(bikeRepository.count()).isEqualTo(0);
	}

	static class Neo4jContainerBasedTestPropertyProvider
			implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues.of(
				"org.neo4j.driver.uri = " + neo4jServer.getBoltUrl(),
				"org.neo4j.driver.authentication.username = neo4j",
				"org.neo4j.driver.authentication.password = " + neo4jServer.getAdminPassword()
			).applyTo(applicationContext.getEnvironment());
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@ImportAutoConfiguration({ Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class,
		Neo4jRepositoriesAutoConfiguration.class })
	static class TestConfiguration {
	}
}
