/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.template.context;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.events.*;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Configuration bean for testing data manipulation events supported by <code>Neo4jTemplate</code>.
 *
 * @author Adam George
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@Configuration
@EnableNeo4jRepositories
@EnableTransactionManagement
public class DataManipulationEventConfiguration {

	@Bean
	public PlatformTransactionManager transactionManager() {
		return new Neo4jTransactionManager(sessionFactory());
	}

	@Bean
	public SessionFactory sessionFactory() {
		return new SessionFactory("org.springframework.data.neo4j.examples.movies.domain"){

			@Override
			public Session openSession() {
				Session session = super.openSession();
				session.register(eventPublisher());
				return session;
			}
		};
	}

	@Bean
	public EventPublisher eventPublisher() {
		return new EventPublisher();
	}

	@Bean
	public Neo4jModificationEventListener eventListener() {
		return new Neo4jModificationEventListener();
	}
}
