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
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeDeleteEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
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

	@Autowired
	private Session session;

	@Autowired
	private TestNeo4jEventListener<BeforeSaveEvent> beforeSaveEventListener;
	@Autowired
	private TestNeo4jEventListener<AfterSaveEvent> afterSaveEventListener;
	@Autowired
	private TestNeo4jEventListener<BeforeDeleteEvent> beforeDeleteEventListener;
	@Autowired
	private TestNeo4jEventListener<AfterDeleteEvent> afterDeleteEventListener;

	@Test
	@Transactional
	public void shouldCreateTemplateAndPublishAppropriateApplicationEventsOnSaveAndOnDelete() {
		assertNotNull("The Neo4jTemplate wasn't autowired into this test", this.session);

		Actor entity = new Actor();
		entity.setName("John Abraham");

		assertFalse(this.beforeSaveEventListener.hasReceivedAnEvent());
		assertFalse(this.afterSaveEventListener.hasReceivedAnEvent());
		this.session.save(entity);
		assertTrue(this.beforeSaveEventListener.hasReceivedAnEvent());
		assertSame(entity, this.beforeSaveEventListener.getEvent().getEntity());
		assertTrue(this.afterSaveEventListener.hasReceivedAnEvent());
		assertSame(entity, this.afterSaveEventListener.getEvent().getEntity());

		assertFalse(this.beforeDeleteEventListener.hasReceivedAnEvent());
		assertFalse(this.afterDeleteEventListener.hasReceivedAnEvent());
		this.session.delete(entity);
		assertTrue(this.beforeDeleteEventListener.hasReceivedAnEvent());
		assertSame(entity, this.beforeDeleteEventListener.getEvent().getEntity());
		assertTrue(this.afterDeleteEventListener.hasReceivedAnEvent());
		assertSame(entity, this.afterDeleteEventListener.getEvent().getEntity());
	}
}
