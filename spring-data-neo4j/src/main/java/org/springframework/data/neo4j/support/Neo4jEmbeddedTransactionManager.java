package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.*;

import javax.transaction.*;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.util.Stack;

/**
 * @author mh
 * @since 28.09.14
 */
public class Neo4jEmbeddedTransactionManager implements TransactionManager, Transaction {

    static class TxState {
        private int state = Status.STATUS_NO_TRANSACTION;
        private org.neo4j.graphdb.Transaction tx;

        boolean isActive() {
            return state == Status.STATUS_ACTIVE || state == Status.STATUS_MARKED_ROLLBACK;
        }
        static TxState begin(org.neo4j.graphdb.Transaction tx) {
            TxState state = new TxState();
            state.tx = tx;
            state.state = Status.STATUS_ACTIVE;
            return state;
        }

        public void rollback() {
            state = Status.STATUS_ROLLING_BACK;
            tx.failure();
            tx.close();
            state = Status.STATUS_ROLLEDBACK;
        }
        public void commit() {
            state = Status.STATUS_COMMITTING;
            tx.success();
            tx.close();
            state = Status.STATUS_COMMITTED;
        }

        public void setRollbackOnly() {
            tx.failure();
            state = Status.STATUS_MARKED_ROLLBACK;
        }
    }
    private static final ThreadLocal<Stack<TxState>> transaction = new ThreadLocal<Stack<TxState>>() {
        @Override
        protected Stack<TxState> initialValue() {
            return new Stack<>();
        }
    };
    private final GraphDatabaseService graphDatabaseService;

    public Neo4jEmbeddedTransactionManager(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }


    @Override
    public void begin() throws NotSupportedException, SystemException {
        beginTx();
    }

    public org.neo4j.graphdb.Transaction beginTx() {
        org.neo4j.graphdb.Transaction tx = graphDatabaseService.beginTx();
        stack().push(TxState.begin(tx));
        return tx;
    }

    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        TxState tx = getTxState();
        if (tx == null) throw new NotInTransactionException("Not in transaction");
        try {
            tx.commit();
        } finally {
            stack().pop();
        }
    }

    @Override
    public boolean delistResource(XAResource xaRes, int flag) throws IllegalStateException, SystemException {
        return false;
    }

    @Override
    public boolean enlistResource(XAResource xaRes) throws IllegalStateException, RollbackException, SystemException {
        return false;
    }

    @Override
    public void registerSynchronization(Synchronization synch) throws IllegalStateException, RollbackException, SystemException {

    }

    @Override
    public int getStatus() throws SystemException {
        TxState state = getTxState();
        if (state == null) return Status.STATUS_NO_TRANSACTION;
        return state.state;
    }


    @Override
    public Transaction getTransaction() throws SystemException {
        return this;
    }

    /*
    int STATUS_ACTIVE = 0;
    int STATUS_MARKED_ROLLBACK = 1;
    int STATUS_COMMITTED = 3;
    int STATUS_ROLLEDBACK = 4;
    int STATUS_UNKNOWN = 5;
    int STATUS_NO_TRANSACTION = 6;
    int STATUS_COMMITTING = 8;
    int STATUS_ROLLING_BACK = 9;
     */
    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        TxState state = getTxState();
        if (state!=null) {
            try {
                state.rollback();
            } finally {
                stack().pop();
            }
        }
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        TxState state = getTxState();
        state.setRollbackOnly();
        getNeo4jTransaction().failure();
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
    }

    @Override
    public Transaction suspend() throws SystemException {
        return this;
    }

    @Override
    public void resume(Transaction tx) throws IllegalStateException, InvalidTransactionException, SystemException {
    }

    public boolean isActive() {
        TxState tx = getTxState();
        return tx !=null && tx.isActive();
    }

    public org.neo4j.graphdb.Transaction getNeo4jTransaction() {
        return getTxState().tx;
    }

    protected TxState getTxState() {
        return stack().isEmpty() ? null : stack().peek();
    }

    protected Stack<TxState> stack() {
        return transaction.get();
    }
}
