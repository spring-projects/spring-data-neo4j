/*
 * Copyright 2011-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.nativetypes;

import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Shared configuration for all tests involving native, spatial types.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Configuration
@EnableNeo4jRepositories
@EnableTransactionManagement
class SpatialPersistenceContextConfiguration {

	@Bean
	ServerControls neo4j() {
		return TestServerBuilders.newInProcessBuilder().newServer();
	}

	@Bean
	org.neo4j.ogm.config.Configuration neo4jOGMConfiguration(ServerControls serverControls) {
		return new org.neo4j.ogm.config.Configuration.Builder() //
				.uri(serverControls.boltURI().toString()) //
				.useNativeTypes() //
				.build();
	}

	@Bean
	public SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration configuration) {
		return new SessionFactory(configuration, "org.springframework.data.neo4j.nativetypes");
	}

	@Bean
	public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
		return new Neo4jTransactionManager(sessionFactory);
	}
}
