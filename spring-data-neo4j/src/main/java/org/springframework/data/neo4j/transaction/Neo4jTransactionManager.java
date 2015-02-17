package org.springframework.data.neo4j.transaction;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class Neo4jTransactionManager implements PlatformTransactionManager {

    private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionManager.class);
    private final Session session;

    public Neo4jTransactionManager(Session session) {
        this.session = session;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
        logger.info("Requesting new transaction");
        return new Neo4jTransactionStatus(session.beginTransaction());
    }

    @Override
    public void commit(TransactionStatus transactionStatus) throws TransactionException {
        Transaction tx = ((Neo4jTransactionStatus) transactionStatus).getTransaction();
        logger.info("Commit requested: " + tx.url() + ", status: " + tx.status().toString());
        if (tx.status() == (Transaction.Status.PENDING) || tx.status() == (Transaction.Status.OPEN)) {
            logger.info("Commit invoked");
            tx.commit();
        }
    }

    @Override
    public void rollback(TransactionStatus transactionStatus) throws TransactionException {
        Transaction tx = ((Neo4jTransactionStatus) transactionStatus).getTransaction();
        logger.info("Rollback requested: " + tx.url() + ", status: " + tx.status().toString());
        if (tx.status() == (Transaction.Status.PENDING) || tx.status() == (Transaction.Status.OPEN)) {
            logger.info("Rollback invoked");
            tx.rollback();
        }
    }
}
