package org.springframework.data.neo4j.event;

/**
 * {@link Neo4jDataManipulationEvent} published before a particular entity is deleted.
 */
public class BeforeDeleteEvent extends Neo4jDataManipulationEvent {

    private static final long serialVersionUID = 1238219872542331942L;

    public BeforeDeleteEvent(Object source, Object entity) {
        super(source, entity);
    }

}
