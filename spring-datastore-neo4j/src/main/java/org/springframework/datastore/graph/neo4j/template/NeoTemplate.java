package org.springframework.datastore.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.datastore.graph.neo4j.template.graph.NeoGraph;

public class NeoTemplate
{
    private final GraphDatabaseService neo;

    public NeoTemplate(final GraphDatabaseService neo)
    {
        if (neo == null)
            throw new IllegalArgumentException("NeoService must not be null");
        this.neo = neo;
    }

    public void execute(final NeoCallback callback)
    {
        if (callback == null)
            throw new IllegalArgumentException("Callback must not be null");
        final TransactionStatus status = new TransactionStatus(neo);
        try
        {

            callback.neo(status, new NeoGraph(neo));
        } catch (Exception e)
        {
            status.mustRollback();
            throw new RuntimeException("Error executing neo callback " + callback, e);
        } finally
        {
            status.finish();
        }
    }

    private static class TransactionStatus implements NeoCallback.Status
    {
        private boolean rollback;
        private final GraphDatabaseService neo;
        private Transaction tx;

        private TransactionStatus(final GraphDatabaseService neo)
        {
            if (neo == null)
                throw new IllegalArgumentException("NeoService must not be null");
            this.neo = neo;
            begin();
        }

        private void begin()
        {
            tx = neo.beginTx();
        }

        public void mustRollback()
        {
            this.rollback = true;
        }

        public void interimCommit()
        {
            finish();
            begin();
        }

        private void finish()
        {
            try
            {
                if (rollback)
                {
                    tx.failure();
                } else
                {
                    tx.success();
                }
            }
            finally
            {
                tx.finish();
            }
        }
    }
}
