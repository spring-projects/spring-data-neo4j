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

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.neo4j_rx.bikes.BikeNode;
import org.springframework.boot.autoconfigure.data.neo4j_rx.bikes.BikeRepository;
import org.springframework.boot.autoconfigure.data.neo4j_rx.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;

/**
 * @author Michael J. Simons
 */
class Neo4jRepositoriesAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(Neo4jDataAutoConfigurationTest.MockedDriverConfiguration.class)
		.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class,
			Neo4jRepositoriesAutoConfiguration.class));

	@Test
	public void defaultRepositoryConfigurationShouldWork() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.run(ctx -> assertThat(ctx).hasSingleBean(BikeRepository.class));
	}

	@Test
	public void repositoryConfigurationShouldNotCreateArbitraryRepos() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.run(ctx ->
				assertThat(ctx)
					.hasSingleBean(Neo4jTransactionManager.class)
					.doesNotHaveBean(Neo4jRepository.class)
			);
	}

	@Test
	public void configurationOfRepositoryTypeShouldWork() {
		this.contextRunner
			.withPropertyValues("spring.data.neo4j.repositories.type=none")
			.withUserConfiguration(TestConfiguration.class)
			.run(ctx ->
				assertThat(ctx)
					.hasSingleBean(Neo4jTransactionManager.class)
					.doesNotHaveBean(Neo4jRepository.class)
			);

		this.contextRunner
			.withPropertyValues("spring.data.neo4j.repositories.type=reactive")
			.withUserConfiguration(TestConfiguration.class)
			.run(ctx ->
				assertThat(ctx)
					.hasSingleBean(Neo4jTransactionManager.class)
					.doesNotHaveBean(Neo4jRepository.class)
			);
	}

	@Test
	public void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class)
			.run(ctx -> assertThat(ctx)
				.hasSingleBean(Neo4jTransactionManager.class)
				.doesNotHaveBean(Neo4jRepository.class));
	}

	@Configuration
	@TestAutoConfigurationPackage(BikeNode.class)
	static class TestConfiguration {
	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	static class EmptyConfiguration {
	}

	@Configuration
	@EnableNeo4jRepositories("no.repositories.here")
	@TestAutoConfigurationPackage(Neo4jRepositoriesAutoConfigurationTest.class)
	static class SortOfInvalidCustomConfiguration {
	}
}
