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
package org.neo4j.springframework.boot.test.autoconfigure.data;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Neo4jContainer;

/**
 * Integration tests for the SDN/RX Neo4j test slice.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
class DataNeo4jTestIT {

	@Nested
	@DisplayName("Default usage with test harness")
	@DataNeo4jTest
	class DriverBasedOnTestHarness extends TestBase {
	}

	@Nested
	@DisplayName("Properties should be applied.")
	@DataNeo4jTest(properties = "spring.profiles.active=test")
	class DataNeo4jTestPropertiesIT {

		@Autowired
		private Environment environment;

		@Test
		void environmentWithNewProfile() {
			assertThat(this.environment.getActiveProfiles()).containsExactly("test");
		}
	}

	@Nested
	@DisplayName("Include filter should work")
	@DataNeo4jTest(includeFilters = @ComponentScan.Filter(Service.class))
	class DataNeo4jTestWithIncludeFilterIntegrationTests {

		@Autowired
		private ExampleService service;

		@Test
		void testService() {
			assertThat(this.service.hasNode(ExampleEntity.class)).isFalse();
		}
	}

	@Nested
	@DisplayName("Usage with driver auto configuration")
	// TODO We don't use @Containers, but the new attribute is helpful to prevent unnecessary failing tests.
	// @Testcontainers(disabledWithoutDocker = true)
	@ContextConfiguration(initializers = TestContainerInitializer.class)
	@DataNeo4jTest(excludeAutoConfiguration = Neo4jTestHarnessAutoConfiguration.class)
	class DriverBasedOnAutoConfiguration extends TestBase {
	}

	/**
	 * An initializer that starts a Neo4j test container and sets {@code org.neo4j.driver.uri} to the containers
	 * bolt uri. It also registers an application listener that stops the container when the context closes.
	 */
	static class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>().withoutAuthentication();
			neo4jContainer.start();
			configurableApplicationContext
				.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> neo4jContainer.stop());
			TestPropertyValues.of("org.neo4j.driver.uri=" + neo4jContainer.getBoltUrl())
				.applyTo(configurableApplicationContext.getEnvironment());
		}
	}

	/**
	 * Tests for both scenarios.
	 */
	abstract class TestBase {
		@Autowired
		private Driver driver;

		@Autowired
		private ExampleRepository exampleRepository;

		@Autowired
		private ApplicationContext applicationContext;

		@Test
		void testRepository() {
			ExampleEntity entity = new ExampleEntity("Look, new @DataNeo4jTest!");
			assertThat(entity.getId()).isNull();
			ExampleEntity persistedEntity = this.exampleRepository.save(entity);
			assertThat(persistedEntity.getId()).isNotNull();

			try (Session session = driver
				.session(SessionConfig.builder().withDefaultAccessMode(AccessMode.READ).build())) {
				long cnt = session.run("MATCH (n:ExampleEntity) RETURN count(n) as cnt").single().get("cnt").asLong();
				assertThat(cnt).isEqualTo(1L);
			}
		}

		@Test
		void didNotInjectExampleService() {
			assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
		}
	}
}
