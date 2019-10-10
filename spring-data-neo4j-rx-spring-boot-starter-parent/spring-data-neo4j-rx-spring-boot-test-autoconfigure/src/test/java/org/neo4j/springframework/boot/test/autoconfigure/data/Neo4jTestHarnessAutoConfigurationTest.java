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
import static org.mockito.Mockito.*;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.harness.ServerControls;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Michael J. Simons
 */
class Neo4jTestHarnessAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Neo4jTestHarnessAutoConfiguration.class));

	@Test
	void shouldRequireTestHarness() {
		contextRunner
			.withClassLoader(new FilteredClassLoader(ServerControls.class))
			.run(ctx -> assertThat(ctx)
				.doesNotHaveBean(ServerControls.class)
				.doesNotHaveBean(Driver.class));
	}

	@Test
	void shouldCreateServerControlsAndDriver() {
		contextRunner
			.run(ctx -> assertThat(ctx)
				.hasSingleBean(ServerControls.class)
				.hasSingleBean(Driver.class));
	}

	@Test
	void existingDriverShouldHavePrecedence() {
		contextRunner
			.withUserConfiguration(ConfigurationWithDriver.class)
			.run(ctx -> {
				assertThat(ctx)
					.doesNotHaveBean(ServerControls.class)
					.hasSingleBean(Driver.class);

				Driver driver = ctx.getBean(Driver.class);
				driver.session();
				verify(driver).session();
			});
	}

	@Test
	void existingServerControlsShouldHavePrecedence() {
		contextRunner
			.withUserConfiguration(ConfigurationWithServerControls.class)
			.run(ctx -> {
				assertThat(ctx)
					.hasSingleBean(ServerControls.class)
					.hasSingleBean(Driver.class);

				verify(ctx.getBean(ServerControls.class)).boltURI();
			});
	}

	@Configuration
	static class ConfigurationWithDriver {

		@Bean
		Driver neo4jDriver() {
			return mock(Driver.class);
		}
	}

	@Configuration
	static class ConfigurationWithServerControls {

		@Bean
		ServerControls serverControls() {
			final ServerControls mockedServerControls = mock(ServerControls.class);
			when(mockedServerControls.boltURI()).thenReturn(URI.create("bolt://localhost:4711"));
			return mockedServerControls;
		}
	}
}
