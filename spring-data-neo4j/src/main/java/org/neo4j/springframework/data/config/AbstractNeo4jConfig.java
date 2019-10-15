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
package org.neo4j.springframework.data.config;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.Neo4jTemplate;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.neo4j.springframework.data.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base class for imperative SDN-RX configuration using JavaConfig.
 * This can be included in all scenarios in which Spring Boot is not an option.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@Configuration
@API(status = API.Status.STABLE, since = "1.0")
public abstract class AbstractNeo4jConfig extends Neo4jConfigurationSupport {

	/**
	 * The driver to be used for interacting with Neo4j.
	 *
	 * @return the Neo4j Java driver instance to work with.
	 */
	public abstract Driver driver();

	/**
	 * The driver used here should be the driver resulting from {@link #driver()}, which is the default.
	 *
	 * @param driver The driver to connect with.
	 * @return A imperative Neo4j client.
	 */
	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	public Neo4jClient neo4jClient(Driver driver) {
		return Neo4jClient.create(driver);
	}

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	public Neo4jTemplate neo4jTemplate(final Neo4jClient neo4jClient, final Neo4jMappingContext mappingContext) {
		return new Neo4jTemplate(neo4jClient, mappingContext);
	}

	/**
	 * Provides a {@link PlatformTransactionManager} for Neo4j based on the driver resulting from {@link #driver()}.
	 *
	 * @param driver The driver to synchronize against
	 * @return A platform transaction manager
	 */
	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	public PlatformTransactionManager transactionManager(Driver driver) {

		return new Neo4jTransactionManager(driver);
	}
}
