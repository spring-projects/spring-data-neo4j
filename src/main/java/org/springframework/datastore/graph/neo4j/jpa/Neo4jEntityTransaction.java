package org.springframework.datastore.graph.neo4j.jpa;

import javax.persistence.EntityTransaction;
import javax.transaction.*;

/**
 * @author Michael Hunger
 * @since 20.08.2010
 */
class Neo4jEntityTransaction implements EntityTransaction {
    private final TransactionManager transactionManager;

    public Neo4jEntityTransaction(final TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void begin() {
        try {
            transactionManager.begin();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        } catch (NotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void commit() {
        try {
            transactionManager.commit();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        } catch (HeuristicRollbackException e) {
            throw new IllegalStateException(e);
        } catch (HeuristicMixedException e) {
            throw new IllegalStateException(e);
        } catch (RollbackException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void rollback() {
        try {
            transactionManager.rollback();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setRollbackOnly() {
        try {
            transactionManager.setRollbackOnly();
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean getRollbackOnly() {
        try {
            return transactionManager.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isActive() {
        try {
            return transactionManager.getStatus() == Status.STATUS_ACTIVE;
        } catch (SystemException e) {
            throw new IllegalStateException(e);
        }
    }
}
