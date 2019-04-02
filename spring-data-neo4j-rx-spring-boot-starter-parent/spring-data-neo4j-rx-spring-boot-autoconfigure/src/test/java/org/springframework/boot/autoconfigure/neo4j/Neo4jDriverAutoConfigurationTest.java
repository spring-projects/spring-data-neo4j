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
package org.springframework.boot.autoconfigure.neo4j;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverFactory.DefaultDriverFactory;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverFactory.RoutingDriverFactory;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * @author Michael J. Simons
 */
class Neo4jDriverAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class));

	@Test
	void shouldRequireAllNeededClasses() {

		contextRunner
			.withClassLoader(new FilteredClassLoader(Driver.class))
			.run((ctx) -> assertThat(ctx).doesNotHaveBean(DefaultDriverFactory.class));
	}

	@Test
	void shouldCreateDefaultDriverFactoryWithDefaultUri() {

		contextRunner
			.run(ctx -> assertThat(ctx)
				.hasSingleBean(DefaultDriverFactory.class)
			);
	}

	@Test
	void shouldCreateDefaultDriverFactoryForSingleUri() {

		contextRunner
			.withPropertyValues("spring.neo4j.uri=bolt://localhost:4711")
			.run(ctx -> assertThat(ctx)
				.hasSingleBean(DefaultDriverFactory.class)
			);
	}

	@Test
	void shouldCreateDefaultDriverFactoryForSingleElementUris() {

		contextRunner
			.withPropertyValues("spring.neo4j.uris=bolt://localhost:4711")
			.run(ctx -> assertThat(ctx)
				.hasSingleBean(DefaultDriverFactory.class)
			);
	}

	@Test
	void shouldCreateRoutingDriverFactoryForMultipleUris() {

		contextRunner
			.withPropertyValues(
				"spring.neo4j.uris=bolt+routing://instance1:7687, bolt+routing://instance2:7687"
			)
			.run(ctx -> assertThat(ctx)
				.hasSingleBean(RoutingDriverFactory.class)
			);
	}
}
