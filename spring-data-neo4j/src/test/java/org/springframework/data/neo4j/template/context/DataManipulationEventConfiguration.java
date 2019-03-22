/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.template.context;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.events.EventPublisher;
import org.springframework.data.neo4j.events.Neo4jModificationEventListener;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
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
		return new SessionFactory("org.springframework.data.neo4j.examples.movies.domain") {

			@Override
			public Session openSession() {
				Session session = super.openSession();
				session.register(eventPublisher());
				return session;
			}
		};
	}

	@Bean
	public Neo4jOperations template() {
		return new Neo4jTemplate(sessionFactory());
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
