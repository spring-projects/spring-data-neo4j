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

package org.springframework.data.neo4j.template;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.events.EventPublisher;
import org.springframework.data.neo4j.events.Neo4jModificationEventListener;
import org.springframework.data.neo4j.examples.movies.domain.Actor;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test to assert the behaviour of {@link Session}'s interaction with Spring application events.
 *
 * @author Adam George
 * @author Mark Angrish
 */
@ContextConfiguration(classes = TemplateApplicationEventTests.DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TemplateApplicationEventTests extends MultiDriverTestClass {

	@Autowired private Session session;

	@Autowired private Neo4jModificationEventListener eventListener;

	@Test
	@Transactional
	public void shouldCreateTemplateAndPublishAppropriateApplicationEventsOnSaveAndOnDelete() {
		assertNotNull("The Neo4jTemplate wasn't autowired into this test", this.session);

		Actor entity = new Actor();
		entity.setName("John Abraham");

		assertFalse(this.eventListener.receivedPreSaveEvent());
		assertFalse(this.eventListener.receivedPostSaveEvent());
		this.session.save(entity);
		assertTrue(this.eventListener.receivedPreSaveEvent());

		assertSame(entity, this.eventListener.getPreSaveEvent().getSource().getObject());
		assertTrue(this.eventListener.receivedPostSaveEvent());
		assertSame(entity, this.eventListener.getPostSaveEvent().getSource().getObject());

		assertFalse(this.eventListener.receivedPreDeleteEvent());
		assertFalse(this.eventListener.receivedPostDeleteEvent());
		this.session.delete(entity);
		assertTrue(this.eventListener.receivedPreDeleteEvent());
		assertSame(entity, this.eventListener.getPreDeleteEvent().getSource().getObject());
		assertTrue(this.eventListener.receivedPostDeleteEvent());
		assertSame(entity, this.eventListener.getPostDeleteEvent().getSource().getObject());
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class DataManipulationEventConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.movies.domain") {

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

}
