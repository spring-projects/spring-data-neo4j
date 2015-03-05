package org.springframework.data.neo4j.event;

/**
 * {@link Neo4jDataManipulationEvent} published after a particular entity is deleted.
 */
public class AfterDeleteEvent extends Neo4jDataManipulationEvent {

    private static final long serialVersionUID = 1185473862611150682L;

    public AfterDeleteEvent(Object source, Object entity) {
        super(source, entity);
    }

}
