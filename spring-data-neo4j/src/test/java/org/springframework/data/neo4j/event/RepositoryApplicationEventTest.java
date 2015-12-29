/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.event;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.event.context.DataManipulationEventConfiguration;
import org.springframework.data.neo4j.event.context.TestNeo4jEventListener;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Actor;
import org.springframework.data.neo4j.examples.movies.repo.ActorRepository;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;

@ContextConfiguration(classes = {MoviesContext.class, DataManipulationEventConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class RepositoryApplicationEventTest extends MultiDriverTestClass {

    @Autowired
    private ActorRepository actorRepository;

    @Autowired
    private TestNeo4jEventListener<BeforeSaveEvent> beforeSaveEventListener;
    @Autowired
    private TestNeo4jEventListener<AfterSaveEvent> afterSaveEventListener;
    @Autowired
    private TestNeo4jEventListener<BeforeDeleteEvent> beforeDeleteEventListener;
    @Autowired
    private TestNeo4jEventListener<AfterDeleteEvent> afterDeleteEventListener;

    @Test
    public void shouldSaveGraphBackedEntityAndPublishAppropriateApplicationEventsOnSaveAndOnDelete() {
        Actor entity = new Actor();
        entity.setName("John Abraham");
        
        assertEquals(0, IterableUtils.count(actorRepository.findAll()));
        assertFalse(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertFalse(this.afterSaveEventListener.hasReceivedAnEvent());
        actorRepository.save(entity);
        assertEquals(1, IterableUtils.count(actorRepository.findAll()));
        assertTrue(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertSame(entity, this.beforeSaveEventListener.getEvent().getEntity());
        assertTrue(this.afterSaveEventListener.hasReceivedAnEvent());
        assertSame(entity, this.afterSaveEventListener.getEvent().getEntity());

        assertFalse(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertFalse(this.afterDeleteEventListener.hasReceivedAnEvent());
        actorRepository.delete(entity);
        assertTrue(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertSame(entity, this.beforeDeleteEventListener.getEvent().getEntity());
        assertTrue(this.afterDeleteEventListener.hasReceivedAnEvent());
        assertSame(entity, this.afterDeleteEventListener.getEvent().getEntity());
    }

}
