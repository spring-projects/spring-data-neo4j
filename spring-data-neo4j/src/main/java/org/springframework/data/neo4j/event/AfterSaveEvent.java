package org.springframework.data.neo4j.event;

/**
 * {@link Neo4jDataManipulationEvent} published after a particular entity is saved.
 */
public class AfterSaveEvent extends Neo4jDataManipulationEvent {

    private static final long serialVersionUID = 894064891865991948L;

    public AfterSaveEvent(Object source, Object entity) {
        super(source, entity);
    }

}
