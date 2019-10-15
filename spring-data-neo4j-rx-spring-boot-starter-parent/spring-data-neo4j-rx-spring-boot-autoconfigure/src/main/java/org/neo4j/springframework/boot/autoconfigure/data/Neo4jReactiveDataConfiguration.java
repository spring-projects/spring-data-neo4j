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

import static org.springframework.boot.autoconfigure.data.RepositoryType.*;

import reactor.core.publisher.Flux;

import org.neo4j.driver.Driver;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jOperations;
import org.neo4j.springframework.data.core.ReactiveNeo4jTemplate;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager;
import org.neo4j.springframework.data.repository.config.ReactiveNeo4jRepositoryConfigurationExtension;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Internal configuration for the reactive Neo4j client.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ ReactiveNeo4jTransactionManager.class, ReactiveTransactionManager.class, Flux.class })
@ConditionalOnRepositoryType(store = "neo4j", type = REACTIVE)
@AutoConfigureAfter(Neo4jDriverAutoConfiguration.class)
@AutoConfigureBefore(Neo4jReactiveRepositoriesConfiguration.class)
class Neo4jReactiveDataConfiguration {

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	@ConditionalOnMissingBean
	public ReactiveNeo4jClient neo4jClient(Driver driver) {
		return ReactiveNeo4jClient.create(driver);
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	@ConditionalOnMissingBean(ReactiveNeo4jOperations.class)
	public ReactiveNeo4jTemplate neo4jTemplate(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext neo4jMappingContext) {
		return new ReactiveNeo4jTemplate(neo4jClient, neo4jMappingContext);
	}

	@Bean(ReactiveNeo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	@ConditionalOnMissingBean(ReactiveTransactionManager.class)
	public ReactiveTransactionManager transactionManager(Driver driver) {

		return new ReactiveNeo4jTransactionManager(driver);
	}
}
