package org.springframework.data.neo4j.integration.template;

import org.springframework.context.ApplicationListener;
import org.springframework.data.neo4j.event.Neo4jDataManipulationEvent;

/**
 * Spring {@code ApplicationListener} used to capture {@link Neo4jDataManipulationEvent}s that occur during a test run.
 * Note that this is abstract because you're supposed to create an anonymous subclass to handle event type 'E' when you
 * use it.  This ensures Spring doesn't just send {@link Neo4jDataManipulationEvent}s to everything regardless.
 */
public abstract class TestNeo4jEventListener<E extends Neo4jDataManipulationEvent> implements ApplicationListener<E> {

    private Neo4jDataManipulationEvent event;

    @Override
    public void onApplicationEvent(E event) {
        this.event = event;
    }

    public boolean hasReceivedAnEvent() {
        return this.event != null;
    }

    public Neo4jDataManipulationEvent getEvent() {
        return event;
    }

}
