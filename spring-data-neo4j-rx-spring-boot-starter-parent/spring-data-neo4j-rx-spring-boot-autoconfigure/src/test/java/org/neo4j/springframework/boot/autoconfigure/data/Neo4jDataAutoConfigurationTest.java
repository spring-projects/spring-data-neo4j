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
package org.neo4j.springframework.boot.autoconfigure.data;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.Neo4jOperations;
import org.neo4j.springframework.data.core.Neo4jTemplate;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael J. Simons
 */
class Neo4jDataAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockedDriverConfiguration.class)
		.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class));

	@Nested
	class Neo4JImperativeDataConfigurationTest {
		@Test
		@DisplayName("Should require all needed classes")
		void shouldRequireAllNeededClasses() {
			contextRunner
				.withClassLoader(
					new FilteredClassLoader(Neo4jTransactionManager.class, PlatformTransactionManager.class))
				.run(ctx -> assertThat(ctx)
					.doesNotHaveBean(Neo4jClient.class)
					.doesNotHaveBean(Neo4jTransactionManager.class)
				);
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfClient {
			@Test
			@DisplayName("…should create new Neo4j Client")
			void shouldCreateNew() {
				contextRunner
					.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
					.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jClient.class));
			}

			@Test
			@DisplayName("…should not replace existing Neo4j Client")
			void shouldNotReplaceExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingClient.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(Neo4jClient.class)
						.hasBean("myCustomClient")
					);
			}
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfTemplate {
			@Test
			@DisplayName("…should create new Neo4j Template")
			void shouldCreateNew() {
				contextRunner
					.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
					.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jTemplate.class));
			}

			@Test
			@DisplayName("…should not replace existing Neo4j Operations")
			void shouldNotReplaceExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingTemplate.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(Neo4jOperations.class)
						.hasBean("myCustomOperations")
					);
			}
		}

		@Nested
		@DisplayName("Automatic configuration…")
		class ConfigurationOfTransactionManager {
			@Test
			@DisplayName("…should create new Neo4j transaction manager")
			void shouldCreateNew() {
				contextRunner
					.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
					.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jTransactionManager.class));
			}

			@Test
			@DisplayName("…should honour existing transaction manager")
			void shouldHonourExisting() {
				contextRunner
					.withUserConfiguration(ConfigurationWithExistingTransactionManager.class)
					.run(ctx -> assertThat(ctx)
						.hasSingleBean(PlatformTransactionManager.class)
						.hasBean("myCustomTransactionManager")
					);
			}
		}
	}

	@Configuration
	static class MockedDriverConfiguration {
		@Bean
		Driver driver() {
			Driver driver = mock(Driver.class);
			TypeSystem typeSystem = mock(TypeSystem.class);
			Session session = mock(Session.class);
			when(driver.defaultTypeSystem()).thenReturn(typeSystem);
			when(driver.session(Mockito.any(SessionConfig.class))).thenReturn(session);
			return driver;
		}
	}

	@Configuration
	static class ConfigurationWithExistingClient {
		@Bean("myCustomClient")
		Neo4jClient neo4jClient(Driver driver) {
			return Neo4jClient.create(driver);
		}
	}

	@Configuration
	static class ConfigurationWithExistingTemplate {
		@Bean("myCustomOperations")
		Neo4jOperations neo4jOperations() {
			return mock(Neo4jOperations.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingTransactionManager {
		@Bean("myCustomTransactionManager")
		PlatformTransactionManager transactionManager() {
			return mock(PlatformTransactionManager.class);
		}
	}
}
