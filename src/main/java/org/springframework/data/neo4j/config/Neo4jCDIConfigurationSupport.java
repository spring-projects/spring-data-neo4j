/*
 * Copyright 2011-2020 the original author or authors.
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

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.springframework.data.mapping.callback.EntityCallback;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.event.BeforeBindCallback;
import org.springframework.data.neo4j.repository.event.IdGeneratingBeforeBindCallback;
import org.springframework.data.neo4j.repository.event.OptimisticLockingBeforeBindCallback;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Support class that can be used as is for all necessary CDI beans or as a blueprint for custom producers.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
@ApplicationScoped
public final class Neo4jCDIConfigurationSupport {

	private final Driver driver;

	@Inject
	public Neo4jCDIConfigurationSupport(final Driver driver) {
		this.driver = driver;
	}

	@Produces
	@Singleton
	public Neo4jConversions neo4jConversions() {
		return new Neo4jConversions();
	}

	@Produces
	@Singleton
	public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions) {
		return new Neo4jMappingContext(neo4JConversions);
	}

	@Produces
	@Singleton
	public Neo4jClient neo4jClient(Driver driver) {
		return Neo4jClient.create(driver);
	}

	@Produces
	@Singleton
	public Neo4jTemplate neo4jTemplate(final Neo4jClient neo4jClient, final Neo4jMappingContext mappingContext,
			DatabaseSelectionProvider databaseNameProvider, Instance<BeforeBindCallback> services) {

		EntityCallbacks entityCallbacks = EntityCallbacks.create(services.stream().toArray(EntityCallback[]::new));
		return new Neo4jTemplate(neo4jClient, mappingContext, databaseNameProvider, entityCallbacks);
	}

	@Produces
	@Singleton
	public BeforeBindCallback<Object> idGeneratingBeforeBindCallback(final Neo4jMappingContext mappingContext) {
		return new IdGeneratingBeforeBindCallback(mappingContext);
	}

	@Produces
	@Singleton
	public BeforeBindCallback<Object> optimisticLockingBeforeBindCallback(final Neo4jMappingContext mappingContext) {
		return new OptimisticLockingBeforeBindCallback(mappingContext);
	}

	@Produces
	@Singleton
	public PlatformTransactionManager transactionManager(Driver driver,
			DatabaseSelectionProvider databaseNameProvider) {

		return new Neo4jTransactionManager(driver, databaseNameProvider);
	}

	@Produces
	@Singleton
	protected DatabaseSelectionProvider neo4jDatabaseNameProvider() {

		return DatabaseSelectionProvider.getDefaultSelectionProvider();
	}
}
