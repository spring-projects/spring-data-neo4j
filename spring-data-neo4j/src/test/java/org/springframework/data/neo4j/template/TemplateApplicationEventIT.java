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

package org.springframework.data.neo4j.template;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.events.Neo4jModificationEventListener;
import org.springframework.data.neo4j.examples.movies.domain.Actor;
import org.springframework.data.neo4j.template.context.DataManipulationEventConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test to assert the behaviour of {@link Neo4jTemplate}'s interaction with Spring application events.
 *
 * @author Adam George
 * @author Mark Angrish
 */
@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TemplateApplicationEventIT extends MultiDriverTestClass {

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
}
