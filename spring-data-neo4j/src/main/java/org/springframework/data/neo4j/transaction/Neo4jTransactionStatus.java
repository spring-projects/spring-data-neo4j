package org.springframework.data.neo4j.transaction;

import org.neo4j.ogm.session.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class Neo4jTransactionStatus implements TransactionStatus {

    private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionStatus.class);
    private final Transaction transaction;

    public Neo4jTransactionStatus(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public boolean isNewTransaction() {
        return transaction.status().equals(Transaction.Status.OPEN);
    }

    @Override
    public boolean hasSavepoint() {
        logger.info("hasSavePoint? - false");
        return false;
    }

    @Override
    public void setRollbackOnly() {
        logger.info("setRollbackOnly - unsupported");
    }

    @Override
    public boolean isRollbackOnly() {
        logger.info("isRollbackOnly? - false");
        return false;
    }

    @Override
    public void flush() {
        logger.info("flush - unsupported");
    }

    @Override
    public boolean isCompleted() {
        return transaction.status().equals(Transaction.Status.ROLLEDBACK) ||
                transaction.status().equals(Transaction.Status.COMMITTED) ||
                transaction.status().equals(Transaction.Status.CLOSED);

    }

    @Override
    public Object createSavepoint() throws TransactionException {
        logger.info("createSavepoint - unsupported");
        return null;
    }

    @Override
    public void rollbackToSavepoint(Object o) throws TransactionException {
        logger.info("rollbackToSavepoint - unsupported");
    }

    @Override
    public void releaseSavepoint(Object o) throws TransactionException {
        logger.info("releaseSavepoint - unsupported");
    }

    public Transaction getTransaction() {
        return transaction;
    }
}
