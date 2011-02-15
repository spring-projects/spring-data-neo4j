package org.springframework.data.graph.neo4j.support;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.junit.Test;
import org.junit.internal.matchers.TypeSafeMatcher;
import org.springframework.transaction.*;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertThat;
import static org.springframework.data.graph.neo4j.support.ChainedTransactionManagerTest.TestPlatformTransactionManager.createFailingTransactionManager;
import static org.springframework.data.graph.neo4j.support.ChainedTransactionManagerTest.TestPlatformTransactionManager.createNonFailingTransactionManager;
import static org.springframework.data.graph.neo4j.support.ChainedTransactionManagerTest.TransactionManagerMatcher.isCommitted;
import static org.springframework.data.graph.neo4j.support.ChainedTransactionManagerTest.TransactionManagerMatcher.wasRolledback;
import static org.springframework.transaction.HeuristicCompletionException.getStateString;

/**
 * @author mh
 * @since 15.02.11
 */
public class ChainedTransactionManagerTest {

    private ChainedTransactionManager tm;


    @Test
    public void shouldCompleteSuccessfully() throws Exception {
        PlatformTransactionManager transactionManager = createNonFailingTransactionManager("single");
        setupTransactionManagers(transactionManager);

        createAndCommitTransaction();

        assertThat(transactionManager, isCommitted());
    }

    @Test
    public void shouldThrowRolledBackExceptionForSingleTMFailure() throws Exception {

        setupTransactionManagers(createFailingTransactionManager("single"));
        try
        {
            createAndCommitTransaction();
            fail("Didn't throw the expected exception");
        } catch (HeuristicCompletionException e){
            assertEquals(HeuristicCompletionException.STATE_ROLLED_BACK, e.getOutcomeState());
        }

    }

    private void setupTransactionManagers(PlatformTransactionManager... transactionManagers) {
        tm = new ChainedTransactionManager(new TestSynchronizationManager(), transactionManagers);
    }

    @Test
    public void shouldCommitAllRegisteredTM() throws Exception {
        PlatformTransactionManager first = createNonFailingTransactionManager("first");
        PlatformTransactionManager second = createNonFailingTransactionManager("second");
        setupTransactionManagers(first, second);
        createAndCommitTransaction();
        assertThat(first, isCommitted());
        assertThat(second, isCommitted());
    }
    @Test
    public void shouldCommitInReverseOrder() throws Exception {
        PlatformTransactionManager first = createNonFailingTransactionManager("first");
        PlatformTransactionManager second = createNonFailingTransactionManager("second");
        setupTransactionManagers(first, second);
        createAndCommitTransaction();
        assertTrue("second tm commited before first ", commitTime(first) >= commitTime(second));

      //  assertThat(second, committedBefore(first));
    }

    private Long commitTime(PlatformTransactionManager transactionManager) {
        return ((TestPlatformTransactionManager)transactionManager).getCommitTime();
    }

    @Test
    public void shouldThrowMixedRolledBackExceptionForNonFirstTMFailure() throws Exception {

        setupTransactionManagers(
                TestPlatformTransactionManager.createFailingTransactionManager("first"),
                createNonFailingTransactionManager("second"));
        try
        {
            createAndCommitTransaction();
            fail("Didn't throw the expected exception");
        } catch (HeuristicCompletionException e){
            assertHeuristicException(HeuristicCompletionException.STATE_MIXED, e.getOutcomeState());
        }
    }

    @Test
    public void shouldRollbackAllTransactionManagers() throws Exception {

        PlatformTransactionManager first = createNonFailingTransactionManager("first");
        PlatformTransactionManager second = createNonFailingTransactionManager("second");
        setupTransactionManagers(first, second);
        createAndRollbackTransaction();
        assertThat(first, wasRolledback());
        assertThat(second, wasRolledback());

    }
    @Test(expected = UnexpectedRollbackException.class )
    public void shouldThrowExceptionOnFailingRollback() throws Exception {
        PlatformTransactionManager first = createFailingTransactionManager("first");
        setupTransactionManagers(first);
        createAndRollbackTransaction();
    }

    private void createAndRollbackTransaction() {
        MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
        tm.rollback(transaction);
    }

    private void assertHeuristicException(final int expected, final int actual) {
        assertEquals(getStateString(expected), getStateString(actual));
    }

    private void createAndCommitTransaction() {
        MultiTransactionStatus transaction = tm.getTransaction(new DefaultTransactionDefinition());
        tm.commit(transaction);
    }

    private static class TestSynchronizationManager implements SynchronizationManager {
        private boolean synchronizationActive;

        @Override
        public void initSynchronization() {
            synchronizationActive=true;
        }

        @Override
        public boolean isSynchronizationActive() {
            return synchronizationActive;
        }

        @Override
        public void clearSynchronization() {
            synchronizationActive=false;
        }
    }

    static class TestPlatformTransactionManager implements PlatformTransactionManager {

        private Long commitTime;
        private String name;
        private Long rollbackTime;

        public TestPlatformTransactionManager(String name) {
            this.name = name;
        }

        @Factory
        static PlatformTransactionManager createFailingTransactionManager(String name) {
            return new TestPlatformTransactionManager(name+"-failing")
            {
                @Override
                public void commit(TransactionStatus status) throws TransactionException {
                    throw new RuntimeException();
                }

                @Override
                public void rollback(TransactionStatus status) throws TransactionException {
                    throw new RuntimeException();
                }
            };
        }

        @Factory
        static PlatformTransactionManager createNonFailingTransactionManager(String name) {
            return new TestPlatformTransactionManager(name+"-non-failing");
        }

        @Override
        public String toString() {
            return name + (isCommitted() ? " (committed) " : " (not committed)");
        }

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
            return new TestTransactionStatus(definition);
        }

        @Override
        public void commit(TransactionStatus status) throws TransactionException {
            commitTime = System.currentTimeMillis();
        }

        @Override
        public void rollback(TransactionStatus status) throws TransactionException {
            rollbackTime = System.currentTimeMillis();
        }

        public boolean isCommitted() {
            return commitTime!=null;
        }
        public boolean wasRolledBack() {
            return rollbackTime!=null;
        }

        public Long getCommitTime() {
            return commitTime;
        }

        private static class TestTransactionStatus implements TransactionStatus {

            public TestTransactionStatus(TransactionDefinition definition) {
            }

            @Override
            public boolean isNewTransaction() {
                return false;
            }

            @Override
            public boolean hasSavepoint() {
                return false;
            }

            @Override
            public void setRollbackOnly() {

            }

            @Override
            public boolean isRollbackOnly() {
                return false;
            }

            @Override
            public void flush() {

            }

            @Override
            public boolean isCompleted() {
                return false;
            }

            @Override
            public Object createSavepoint() throws TransactionException {
                return null;
            }

            @Override
            public void rollbackToSavepoint(Object savepoint) throws TransactionException {

            }

            @Override
            public void releaseSavepoint(Object savepoint) throws TransactionException {

            }
        }
    }

    static class TransactionManagerMatcher extends TypeSafeMatcher<PlatformTransactionManager> {
        private boolean commitCheck;

        public TransactionManagerMatcher(boolean commitCheck) {
            this.commitCheck = commitCheck;
        }

        @Override
        public boolean matchesSafely(PlatformTransactionManager platformTransactionManager) {
            TestPlatformTransactionManager ptm = (TestPlatformTransactionManager) platformTransactionManager;
            if (commitCheck) {
                return ptm.isCommitted();
            } else {
                return ptm.wasRolledBack();
            }

        }

        @Override
        public void describeTo(Description description) {
            description.appendText("that a "+(commitCheck ? "committed":"rolled-back")+" TransactionManager");
        }

        @Factory
        public static TransactionManagerMatcher isCommitted() {
            return new TransactionManagerMatcher(true);
        }
        @Factory
        public static TransactionManagerMatcher wasRolledback() {
            return new TransactionManagerMatcher(false);
        }
    }
}
