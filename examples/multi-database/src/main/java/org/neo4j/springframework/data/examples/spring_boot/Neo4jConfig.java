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
package org.neo4j.springframework.data.examples.spring_boot;

import java.util.Optional;

import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.core.Neo4jDatabaseNameProvider;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.neo4j.springframework.data.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @author Michael J. Simons
 */
@Configuration
public class Neo4jConfig {

	/**
	 * This bean is only active in profile {@literal "selection-by-user"}. The {@link Neo4jDatabaseNameProvider} created here
	 * uses Springs security context to retrieve the authenticated principal and extracts the username. Thus all requests
	 * will use a different database, depending on the user being logged into the application.
	 *
	 * @return A database name provider.
	 */
	@Profile("selection-by-user")
	@Bean
	Neo4jDatabaseNameProvider databaseNameProvider() {

		return () -> Optional.ofNullable(SecurityContextHolder.getContext())
			.map(SecurityContext::getAuthentication)
			.filter(Authentication::isAuthenticated)
			.map(Authentication::getPrincipal)
			.map(User.class::cast)
			.map(User::getUsername);
	}

	@Profile("multiple-transaction-manager")
	@Configuration
	static class MultipleTransactionManager {

		/**
		 * This is gonna be the default transaction manager, it's name corresponds with {@link Neo4jRepositoryConfigurationExtension#DEFAULT_TRANSACTION_MANAGER_BEAN_NAME}.
		 *
		 * @param driver               The driver needed
		 * @param databaseNameProvider Whatever database name provider is configured
		 * @return A transaction manager
		 */
		@Bean
		public Neo4jTransactionManager transactionManager(Driver driver,
			Neo4jDatabaseNameProvider databaseNameProvider) {

			return new Neo4jTransactionManager(driver, databaseNameProvider);
		}

		/**
		 * A 2nd transaction manager for user with another database.
		 *
		 * @param driver The driver needed
		 * @return A transaction manager
		 */
		@Bean
		public Neo4jTransactionManager transactionManagerForOtherDb(Driver driver) {
			return new Neo4jTransactionManager(driver,
				Neo4jDatabaseNameProvider.createStaticDatabaseNameProvider("otherDb"));
		}
	}
}
