/*
 * Copyright 2011-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.config;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.UserSelectionProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base class for imperative SDN configuration using JavaConfig. This can be included in
 * all scenarios in which Spring Boot is not an option.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
@Configuration
@API(status = API.Status.STABLE, since = "6.0")
public abstract class AbstractNeo4jConfig extends Neo4jConfigurationSupport {

	@Autowired
	private ObjectProvider<UserSelectionProvider> userSelectionProviders;

	@Autowired
	private ObjectProvider<Neo4jBookmarkManager> bookmarkManagerProviders;

	@Override
	public Neo4jConversions neo4jConversions() {
		return super.neo4jConversions();
	}

	@Override
	public org.neo4j.cypherdsl.core.renderer.Configuration cypherDslConfiguration() {
		return super.cypherDslConfiguration();
	}

	@Override
	public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions) throws ClassNotFoundException {
		return super.neo4jMappingContext(neo4JConversions);
	}

	/**
	 * The driver to be used for interacting with Neo4j.
	 * @return the Neo4j Java driver instance to work with.
	 */
	public abstract Driver driver();

	/**
	 * The driver used here should be the driver resulting from {@link #driver()}, which
	 * is the default.
	 * @param driver the driver to connect with.
	 * @param databaseSelectionProvider the database selection provider to use.
	 * @return a imperative Neo4j client.
	 */
	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
	public Neo4jClient neo4jClient(Driver driver, @Nullable DatabaseSelectionProvider databaseSelectionProvider) {

		return Neo4jClient.with(driver)
			.withDatabaseSelectionProvider(databaseSelectionProvider)
			.withUserSelectionProvider(this.userSelectionProviders.getIfUnique())
			.withNeo4jBookmarkManager(getBootBookmarkManager())
			.build();
	}

	private Neo4jBookmarkManager getBootBookmarkManager() {
		return this.bookmarkManagerProviders.getIfAvailable(Neo4jBookmarkManager::create);
	}

	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
	public Neo4jOperations neo4jTemplate(final Neo4jClient neo4jClient, final Neo4jMappingContext mappingContext) {

		return new Neo4jTemplate(neo4jClient, mappingContext);
	}

	/**
	 * Provides a {@link PlatformTransactionManager} for Neo4j based on the driver
	 * resulting from {@link #driver()}.
	 * @param driver the driver to synchronize against
	 * @param databaseSelectionProvider the configured database selection provider
	 * @return a platform transaction manager
	 */
	@Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
	public PlatformTransactionManager transactionManager(Driver driver,
			@Nullable DatabaseSelectionProvider databaseSelectionProvider) {

		return Neo4jTransactionManager.with(driver)
			.withDatabaseSelectionProvider(databaseSelectionProvider)
			.withUserSelectionProvider(this.userSelectionProviders.getIfUnique())
			.withBookmarkManager(getBootBookmarkManager())
			.build();
	}

	@Bean
	public Neo4jBookmarkManager bookmarkManager() {
		return Neo4jBookmarkManager.create();
	}

	/**
	 * Configures the database selection provider.
	 * @return the default database name provider, defaulting to the default database on
	 * Neo4j 4.0 and on no default on Neo4j 3.5 and prior.
	 */
	@Bean
	protected DatabaseSelectionProvider databaseSelectionProvider() {

		return DatabaseSelectionProvider.getDefaultSelectionProvider();
	}

}
