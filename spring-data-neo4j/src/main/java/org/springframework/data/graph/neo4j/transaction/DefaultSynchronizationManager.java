package org.springframework.data.graph.neo4j.transaction;

import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author mh
 * @since 15.02.11
 */
public class DefaultSynchronizationManager implements SynchronizationManager {
    @Override
    public void initSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @Override
    public boolean isSynchronizationActive() {
        return TransactionSynchronizationManager.isSynchronizationActive();
    }

    @Override
    public void clearSynchronization() {
        TransactionSynchronizationManager.clear();
    }
}
