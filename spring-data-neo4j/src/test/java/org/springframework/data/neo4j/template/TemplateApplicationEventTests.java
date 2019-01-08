/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.events.EventPublisher;
import org.springframework.data.neo4j.events.Neo4jModificationEventListener;
import org.springframework.data.neo4j.examples.movies.domain.Actor;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test to assert the behaviour of {@link Session}'s interaction with Spring application events.
 *
 * @author Adam George
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = TemplateApplicationEventTests.DataManipulationEventConfiguration.class)
@RunWith(SpringRunner.class)
public class TemplateApplicationEventTests {

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
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.examples.movies.domain")
	static class DataManipulationEventConfiguration {

		@Bean
		public BeanPostProcessor sessionFactoryPostProcessor(EventPublisher eventPublisher) {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof SessionFactory) {
						((SessionFactory) bean).register(eventPublisher);
					}
					return null;
				}
			};
		}

		@Bean
		public EventPublisher eventPublisher(ApplicationEventPublisher applicationEventPublisher) {
			return new EventPublisher(applicationEventPublisher);
		}

		@Bean
		public Neo4jModificationEventListener eventListener() {
			return new Neo4jModificationEventListener();
		}
	}

}
