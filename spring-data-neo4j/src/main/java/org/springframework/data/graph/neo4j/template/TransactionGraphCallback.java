package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.dao.DataAccessException;
import org.springframework.data.graph.UncategorizedGraphStoreException;

/**
 * @author mh
 * @since 18.02.11
 */
public abstract class TransactionGraphCallback implements GraphCallback {

    public static interface Status {
        void mustRollback();

        void interimCommit();
    }

    public abstract void doWithGraph(Status status, GraphDatabaseService graph) throws Exception;

    @Override
    public void doWithGraph(GraphDatabaseService graph) throws Exception {
        final TransactionStatus status = new TransactionStatus(graph);
        try {
            doWithGraph(status, graph);
        } catch (Exception e) {
            status.mustRollback();
            if (e instanceof DataAccessException) throw (DataAccessException) e;
            throw new UncategorizedGraphStoreException("Error transactionally executing callback", e);
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

}
