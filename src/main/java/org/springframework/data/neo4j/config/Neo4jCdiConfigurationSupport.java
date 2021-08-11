/*
 * Copyright 2011-2021 the original author or authors.
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
import org.neo4j.driver.Driver;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

/**
 * Support class that can be used as is for all necessary CDI beans or as a blueprint for custom producers.
 *
 * @author Michael J. Simons
 * @soundtrack Buckethead - SIGIL Soundtrack
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
@ApplicationScoped
class Neo4jCdiConfigurationSupport {

	private <T> T resolve(Instance<T> instance) {
		if (!instance.isAmbiguous()) {
			return instance.get();
		}

		Instance<T> defaultInstance = instance.select(Neo4jCdiExtension.DEFAULT_BEAN);
		return defaultInstance.get();
	}

	@Produces @Builtin @Singleton
	public Neo4jConversions neo4jConversions() {
		return new Neo4jConversions();
	}

	@Produces @Builtin @Singleton
	public DatabaseSelectionProvider databaseSelectionProvider() {

		return DatabaseSelectionProvider.getDefaultSelectionProvider();
	}

	@Produces @Builtin @Singleton
	public Neo4jOperations neo4jOperations(
			@Any Instance<Neo4jClient> neo4jClient,
			@Any Instance<Neo4jMappingContext> mappingContext
	) {
		return new Neo4jTemplate(resolve(neo4jClient), resolve(mappingContext));
	}

	@Produces @Singleton
	public Neo4jClient neo4jClient(@SuppressWarnings("CdiInjectionPointsInspection") Driver driver) {
		return Neo4jClient.create(driver);
	}

	@Produces @Singleton
	public Neo4jMappingContext neo4jMappingContext(@SuppressWarnings("CdiInjectionPointsInspection") Driver driver, @Any Instance<Neo4jConversions> neo4JConversions) {

		return new Neo4jMappingContext(resolve(neo4JConversions), driver.defaultTypeSystem());
	}

	@Produces @Singleton
	public PlatformTransactionManager transactionManager(
			@SuppressWarnings("CdiInjectionPointsInspection") Driver driver, @Any Instance<DatabaseSelectionProvider> databaseNameProvider) {

		return new Neo4jTransactionManager(driver, resolve(databaseNameProvider));
	}
}
