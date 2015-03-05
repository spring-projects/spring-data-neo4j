package org.springframework.data.neo4j.event;

/**
 * {@link Neo4jDataManipulationEvent} published before a particular entity is saved.
 */
public class BeforeSaveEvent extends Neo4jDataManipulationEvent {

    private static final long serialVersionUID = -2413703447883120441L;

    public BeforeSaveEvent(Object source, Object entity) {
        super(source, entity);
    }

}
