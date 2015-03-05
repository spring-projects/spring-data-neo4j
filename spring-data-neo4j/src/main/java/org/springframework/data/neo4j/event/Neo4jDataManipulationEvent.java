package org.springframework.data.neo4j.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.data.neo4j.template.Neo4jTemplate;

/**
 * A Spring {@link ApplicationEvent} that gets published by {@link Neo4jTemplate} to notify interested parties about data
 * manipulation events.  In previous versions of Spring Data Neo4j this was known as <code>Neo4jLifecycleEvent</code> but
 * has been renamed to better describe the nature of the events.
 */
public class Neo4jDataManipulationEvent extends ApplicationEvent {

    private static final long serialVersionUID = -9025087608146228149L;

    private Object entity;

    public Neo4jDataManipulationEvent(Object source, Object entity) {
        super(source);
        this.entity = entity;
    }

    public Object getEntity() {
        return entity;
    }

}
