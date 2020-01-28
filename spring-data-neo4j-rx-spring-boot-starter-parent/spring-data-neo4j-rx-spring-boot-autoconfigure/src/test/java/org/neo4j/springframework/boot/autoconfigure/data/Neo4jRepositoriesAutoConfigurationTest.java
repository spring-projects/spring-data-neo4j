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

import reactor.core.publisher.Flux;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.neo4j.springframework.boot.autoconfigure.data.bikes.BikeNode;
import org.neo4j.springframework.boot.autoconfigure.data.bikes.BikeRepository;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.neo4j_rx.empty.EmptyDataPackage;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/**
 * @author Michael J. Simons
 */
class Neo4jRepositoriesAutoConfigurationTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(Neo4jDataAutoConfigurationTest.MockedDriverConfiguration.class)
		.withConfiguration(AutoConfigurations.of(Neo4jDriverAutoConfiguration.class, Neo4jDataAutoConfiguration.class,
			Neo4jRepositoriesAutoConfiguration.class));

	@Test
	void defaultRepositoryConfigurationShouldWork() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
			.run(ctx -> assertThat(ctx).hasSingleBean(BikeRepository.class));
	}

	@Test
	void repositoryConfigurationShouldNotCreateArbitraryRepos() {
		this.contextRunner.withUserConfiguration(EmptyConfiguration.class)
			.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
			.run(ctx ->
				assertThat(ctx)
					.hasSingleBean(Neo4jTransactionManager.class)
					.doesNotHaveBean(Neo4jRepository.class)
			);
	}

	@Test
	void configurationOfRepositoryTypeShouldWork() {
		this.contextRunner
			.withPropertyValues("spring.data.neo4j.repositories.type=none")
			.withUserConfiguration(TestConfiguration.class)
			.withClassLoader(new FilteredClassLoader(Flux.class))
			.run(ctx ->
				assertThat(ctx)
					.doesNotHaveBean(Neo4jTransactionManager.class)
					.doesNotHaveBean(ReactiveNeo4jClient.class)
					.doesNotHaveBean(Neo4jRepository.class)
			);

		this.contextRunner
			.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
			.withUserConfiguration(TestConfiguration.class)
			.run(ctx ->
				assertThat(ctx)
					.hasSingleBean(Neo4jTransactionManager.class)
					.hasSingleBean(Neo4jClient.class)
					.doesNotHaveBean(ReactiveNeo4jRepository.class)
			);
	}

	@Test
	void autoConfigurationShouldNotKickInEvenIfManualConfigDidNotCreateAnyRepositories() {
		this.contextRunner.withUserConfiguration(SortOfInvalidCustomConfiguration.class)
			.withPropertyValues("spring.data.neo4j.repositories.type=imperative")
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
