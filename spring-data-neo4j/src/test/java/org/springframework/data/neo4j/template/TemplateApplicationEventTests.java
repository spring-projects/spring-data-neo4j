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
