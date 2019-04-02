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
package org.springframework.boot.autoconfigure.data.neo4j_rx;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael J. Simons
 */
class Neo4jDataAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(MockedDriverConfiguration.class)
		.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class));

	@Test
	@DisplayName("Should require all needed classes")
	void shouldRequireAllNeededClasses() {
		contextRunner
			.withClassLoader(
				new FilteredClassLoader(Driver.class, Neo4jTransactionManager.class, PlatformTransactionManager.class))
			.run(ctx -> assertThat(ctx)
				.doesNotHaveBean(Neo4jTemplate.class)
				.doesNotHaveBean(Neo4jTransactionManager.class)
			);
	}

	@Nested
	@DisplayName("Automatic configuration…")
	class ConfigurationOfTemplate {
		@Test
		@DisplayName("…should create new Neo4j template")
		void shouldCreateNew() {
			contextRunner
				.run(ctx -> assertThat(ctx).hasSingleBean(Neo4jTemplate.class));
		}

		@Test
		@DisplayName("…should not replace existing Neo4j template")
		void shouldNotReplaceExisting() {
			contextRunner
				.withUserConfiguration(ConfigurationWithExistingTemplate.class)
				.run(ctx -> assertThat(ctx)
					.hasSingleBean(Neo4jTemplate.class)
					.hasBean("myCustomTemplate")
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

	@Nested
	@DisplayName("Automatic configuration…")
	class ConfigurationOfNodeManagerFactory {
		@Test
		@DisplayName("…should create new Neo4j node manager")
		void shouldCreateNew() {
			contextRunner
				.run(ctx -> assertThat(ctx).hasSingleBean(NodeManagerFactory.class));
		}

		@Test
		@DisplayName("…should honour existing node manager")
		void shouldHonourExisting() {
			contextRunner
				.withUserConfiguration(ConfigurationWithExistingNodeManager.class)
				.run(ctx -> assertThat(ctx)
					.hasSingleBean(NodeManagerFactory.class)
					.hasBean("myNodeManagerFactory")
				);
		}
	}

	@Configuration
	static class MockedDriverConfiguration {
		@Bean
		Driver driver() {
			return Mockito.mock(Driver.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingTemplate {
		@Bean("myCustomTemplate")
		Neo4jTemplate neo4jTemplate(Driver driver) {
			return new Neo4jTemplate(driver);
		}
	}

	@Configuration
	static class ConfigurationWithExistingTransactionManager {
		@Bean("myCustomTransactionManager")
		PlatformTransactionManager transactionManager() {
			return Mockito.mock(PlatformTransactionManager.class);
		}
	}

	@Configuration
	static class ConfigurationWithExistingNodeManager {
		@Bean("myNodeManagerFactory")
		NodeManagerFactory nodeManagerFactory() {
			return Mockito.mock(NodeManagerFactory.class);
		}
	}
}
