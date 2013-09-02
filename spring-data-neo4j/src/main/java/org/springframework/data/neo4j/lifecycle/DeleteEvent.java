package org.springframework.data.neo4j.lifecycle;

/**
 * @deprecated Rather use the AfterDeleteEvent
 * @param <T>
 */
@Deprecated
public class DeleteEvent<T> extends Neo4jLifecycleEvent<T> {
    public DeleteEvent(Object source, T entity) {
        super(source, entity);
    }
}

