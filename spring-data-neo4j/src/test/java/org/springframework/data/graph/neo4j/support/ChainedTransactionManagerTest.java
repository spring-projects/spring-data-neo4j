package org.springframework.data.graph.neo4j.support;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Arrays;

import static junit.framework.Assert.assertTrue;

/**
 * @author mh
 * @since 15.02.11
 */
public class ChainedTransactionManagerTest {

    @Test
    public void shouldCompleteSuccessfully() throws Exception {
        ChainedTransactionManager tm = new ChainedTransactionManager(new NullSynchronizationManager());
        TestPlatformTransactionManager transactionManager = new TestPlatformTransactionManager();
        tm.setTransactionManagers(Arrays.<PlatformTransactionManager>asList(transactionManager));
        MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
        tm.commit(transaction);

        assertTrue("TM didn't commit", transactionManager.isCommited());
    }

    private static class NullSynchronizationManager implements SynchronizationManager {
        @Override
        public void initSynchronization() {

        }

        @Override
        public boolean isSynchronizationActive() {
            return true;
        }

        @Override
        public void clearSynchronization() {

        }
    }

    private static class TestPlatformTransactionManager implements PlatformTransactionManager {

        private boolean commited;

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return null;
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            commited = true;
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {

        }

        public boolean isCommited() {
            return commited;
        }
    }
}
