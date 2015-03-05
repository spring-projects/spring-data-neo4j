package org.springframework.data.neo4j.integration.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeDeleteEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
import org.springframework.data.neo4j.integration.movies.domain.Actor;
import org.springframework.data.neo4j.integration.template.context.DataManipulationEventConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 * Test to assert the behaviour of {@link Neo4jTemplate}'s interaction with Spring application events.
 */
@ContextConfiguration(classes = DataManipulationEventConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class TemplateApplicationEventTest {

    @Autowired
    private Neo4jOperations neo4jTemplate;

    @Autowired
    private TestNeo4jEventListener<BeforeSaveEvent> beforeSaveEventListener;
    @Autowired
    private TestNeo4jEventListener<AfterSaveEvent> afterSaveEventListener;
    @Autowired
    private TestNeo4jEventListener<BeforeDeleteEvent> beforeDeleteEventListener;
    @Autowired
    private TestNeo4jEventListener<AfterDeleteEvent> afterDeleteEventListener;

    @Test
    public void shouldCreateTemplateAndPublishAppropriateApplicationEventsOnSaveAndOnDelete() {
        assertNotNull("The Neo4jTemplate wasn't autowired into this test", this.neo4jTemplate);

        Actor entity = new Actor();
        entity.setName("John Abraham");

        assertFalse(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertFalse(this.afterSaveEventListener.hasReceivedAnEvent());
        this.neo4jTemplate.save(entity);
        assertTrue(this.beforeSaveEventListener.hasReceivedAnEvent());
        assertSame(entity, this.beforeSaveEventListener.getEvent().getEntity());
        assertTrue(this.afterSaveEventListener.hasReceivedAnEvent());
        assertSame(entity, this.afterSaveEventListener.getEvent().getEntity());

        assertFalse(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertFalse(this.afterDeleteEventListener.hasReceivedAnEvent());
        this.neo4jTemplate.delete(entity);
        assertTrue(this.beforeDeleteEventListener.hasReceivedAnEvent());
        assertSame(entity, this.beforeDeleteEventListener.getEvent().getEntity());
        assertTrue(this.afterDeleteEventListener.hasReceivedAnEvent());
        assertSame(entity, this.afterDeleteEventListener.getEvent().getEntity());
    }

}
