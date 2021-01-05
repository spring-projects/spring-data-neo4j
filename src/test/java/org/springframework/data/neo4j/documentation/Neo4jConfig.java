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
package org.springframework.data.neo4j.documentation;

import java.util.Optional;

import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;

// mock classes from Spring Security
class Authentication {
	boolean isAuthenticated() {
		return false;
	}

	User getPrincipal() {
		return null;
	}
}

class SecurityContext {
	Authentication getAuthentication() {
		return null;
	}
}

class SecurityContextHolder {
	static SecurityContext getContext() {
		return null;
	}
}

class User {
	String getUsername() {
		return null;
	}
}

/**
 * @author Michael J. Simons
 */
// tag::faq.multidatabase[]
@Configuration
public class Neo4jConfig {

	// end::faq.multidatabase[]
	/**
	 * This bean is only active in profile {@literal "selection-by-user"}. The {@link DatabaseSelectionProvider} created
	 * here uses Springs security context to retrieve the authenticated principal and extracts the username. Thus all
	 * requests will use a different database, depending on the user being logged into the application.
	 *
	 * @return A database name provider.
	 */
	@Profile("selection-by-user")
	// tag::faq.multidatabase[]
	@Bean
	DatabaseSelectionProvider databaseSelectionProvider() {

		return () -> Optional.ofNullable(SecurityContextHolder.getContext()).map(SecurityContext::getAuthentication)
				.filter(Authentication::isAuthenticated).map(Authentication::getPrincipal).map(User.class::cast)
				.map(User::getUsername).map(DatabaseSelection::byName).orElseGet(DatabaseSelection::undecided);
	}
	// end::faq.multidatabase[]

	@Profile("multiple-transaction-manager")
	@Configuration
	static class MultipleTransactionManager {

		/**
		 * This is gonna be the default transaction manager, it's name corresponds with
		 * {@link Neo4jRepositoryConfigurationExtension#DEFAULT_TRANSACTION_MANAGER_BEAN_NAME}.
		 *
		 * @param driver The driver needed
		 * @param databaseNameProvider Whatever database name provider is configured
		 * @return A transaction manager
		 */
		@Bean
		public Neo4jTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

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
					DatabaseSelectionProvider.createStaticDatabaseSelectionProvider("otherDb"));
		}
	}

	// tag::faq.multidatabase[]
}
// end::faq.multidatabase[]
