package org.neo4j.rest.graphdb.query;

import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.RestAPICypherImpl;
import org.neo4j.rest.graphdb.transaction.RemoteCypherTransaction;

import javax.transaction.*;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

/**
 * @author mh
 * @since 28.09.14
 */
public class RestCypherTransactionManager implements TransactionManager, Transaction {
    private static final ThreadLocal<RemoteCypherTransaction> cypherTransaction = new ThreadLocal<>();
    private final RestAPICypherImpl restAPICypher;

    public RestCypherTransactionManager(RestAPICypherImpl restAPICypher) {
        this.restAPICypher = restAPICypher;
    }


    @Override
    public void begin() throws NotSupportedException, SystemException {
        beginTx();
    }

    public org.neo4j.graphdb.Transaction beginTx() {
        RemoteCypherTransaction tx = cypherTransaction.get();
        if (tx != null && tx.isActive()) {
            tx.beginInner();
        } else {
            CypherTransaction newTx = restAPICypher.newCypherTransaction();
            tx = new RemoteCypherTransaction(newTx);
            cypherTransaction.set(tx);
        }
        return tx;
    }

    @Override
    public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException, RollbackException, SecurityException, SystemException {
        RemoteCypherTransaction tx = getRemoteCypherTransaction();
        tx.success();
        tx.close();
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
        RemoteCypherTransaction tx = getRemoteCypherTransaction();
        if (tx == null || !tx.isActive()) return Status.STATUS_NO_TRANSACTION;
        return tx.getStatus();
    }


    @Override
    public Transaction getTransaction() throws SystemException {
        return this;
    }

    @Override
    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        RemoteCypherTransaction tx = getRemoteCypherTransaction();
        tx.failure();
        tx.close();
    }

    @Override
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        getRemoteCypherTransaction().failure();
    }

    @Override
    public void setTransactionTimeout(int seconds) throws SystemException {
        // todo
    }

    @Override
    public Transaction suspend() throws SystemException {
        return this;
    } // todo we could implement suspend

    @Override
    public void resume(Transaction tx) throws IllegalStateException, InvalidTransactionException, SystemException {
        // todo we could implement suspend
    }

    public boolean isActive() {
        RemoteCypherTransaction tx = getRemoteCypherTransaction();
        return tx !=null && tx.isActive();
    }

    public CypherTransaction getCypherTransaction() {
        return getRemoteCypherTransaction().getTransaction();
    }

    public RemoteCypherTransaction getRemoteCypherTransaction() {
        return cypherTransaction.get();
    }
}
