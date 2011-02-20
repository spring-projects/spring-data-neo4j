package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.data.graph.UncategorizedGraphStoreException;

/**
 * @author mh
 * @since 18.02.11
 */
public abstract class GraphTransactionCallback<T> implements GraphCallback<T> {

    public interface Status {
        void mustRollback();

        void interimCommit();
    }

    public abstract T doWithGraph(Status status, GraphDatabaseService graph) throws Exception;

    @Override
    public T doWithGraph(GraphDatabaseService graph) throws Exception {
        final TransactionStatus status = new TransactionStatus(graph);
        try {
            return doWithGraph(status, graph);
        } catch (Exception e) {
            status.mustRollback();
            throw e;
        } finally {
            status.finish();
        }
    }

    private static class TransactionStatus implements Status {
        private boolean rollback;
        private final GraphDatabaseService neo;
        private Transaction tx;

        private TransactionStatus(final GraphDatabaseService neo) {
            if (neo == null)
                throw new IllegalArgumentException("GraphDatabaseService must not be null");
            this.neo = neo;
            begin();
        }

        private void begin() {
            tx = neo.beginTx();
        }

        public void mustRollback() {
            this.rollback = true;
        }

        public void interimCommit() {
            finish();
            begin();
        }

        private void finish() {
            try {
                if (rollback) {
                    tx.failure();
                } else {
                    tx.success();
                }
            } finally {
                tx.finish();
            }
        }
    }

    /**
     * @author mh
     * @since 19.02.11
     */
    public abstract static class WithoutResult extends GraphTransactionCallback<Void> {
        @Override
        public Void doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
            doWithGraphWithoutResult(status,graph);
            return null;
        }

        public abstract void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception;
    }
}
