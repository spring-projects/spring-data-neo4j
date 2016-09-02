package org.springframework.data.neo4j.transactions;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * @author vince
 */
public class DelegatingTransactionManager implements PlatformTransactionManager {

    private PlatformTransactionManager transactionManager;
    private TransactionDefinition transactionDefinition;

    public DelegatingTransactionManager(PlatformTransactionManager platformTransactionManager) {
        this.transactionManager = platformTransactionManager;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
        this.transactionDefinition = transactionDefinition;
        return transactionManager.getTransaction(transactionDefinition);
    }

    @Override
    public void commit(TransactionStatus transactionStatus) throws TransactionException {
        transactionManager.commit(transactionStatus);
    }

    @Override
    public void rollback(TransactionStatus transactionStatus) throws TransactionException {
        transactionManager.rollback(transactionStatus);
    }

    public TransactionDefinition getTransactionDefinition() {
        return transactionDefinition;
    }
}
