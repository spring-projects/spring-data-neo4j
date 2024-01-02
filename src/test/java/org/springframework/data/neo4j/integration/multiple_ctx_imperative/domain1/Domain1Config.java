/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.multiple_ctx_imperative.domain1;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.neo4j.config.Neo4jEntityScanner;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Michael J. Simons
 * @soundtrack Kelis - Tasty
 */
@Configuration(proxyBeanMethods = false)
@EnableNeo4jRepositories(
		basePackageClasses = Domain1Config.class,
		neo4jMappingContextRef = "domain1Context",
		neo4jTemplateRef = "domain1Template",
		transactionManagerRef = "domain1Manager"
)
public class Domain1Config {

	@Primary @Bean
	public Driver domain1Driver(Environment env) {

		return GraphDatabase.driver(env.getRequiredProperty("database1.url"),
				AuthTokens.basic("neo4j", env.getRequiredProperty("database1.password")));
	}

	@Primary @Bean
	public Neo4jClient domain1Client(@Qualifier("domain1Driver") Driver driver) {
		return Neo4jClient.create(driver);
	}

	@Primary @Bean
	public Neo4jOperations domain1Template(
			@Qualifier("domain1Client") Neo4jClient domain1Client,
			@Qualifier("domain1Context") Neo4jMappingContext domain1Context
	) {
		return new Neo4jTemplate(domain1Client, domain1Context);
	}

	@Primary @Bean
	public PlatformTransactionManager domain1Manager(
			@Qualifier("domain1Driver") Driver driver,
			@Qualifier("domain1Selection") DatabaseSelectionProvider domain1Selection
	) {
		return new Neo4jTransactionManager(driver, domain1Selection);
	}

	@Primary @Bean
	public DatabaseSelectionProvider domain1Selection() {
		return () -> DatabaseSelection.undecided();
	}

	@Primary @Bean
	public Neo4jMappingContext domain1Context(Neo4jConversions neo4jConversions) throws ClassNotFoundException {
		Neo4jMappingContext context = new Neo4jMappingContext(neo4jConversions);
		context.setInitialEntitySet(Neo4jEntityScanner.get().scan(this.getClass().getPackage().getName()));
		context.setStrict(true);
		return context;
	}
}
