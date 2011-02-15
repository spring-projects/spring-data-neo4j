package org.springframework.data.graph.neo4j.support;

/**
 * @author mh
 * @since 15.02.11
 */
public interface SynchronizationManager {
    void initSynchronization();

    boolean isSynchronizationActive();

    void clearSynchronization();
}
