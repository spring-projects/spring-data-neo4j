/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
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
