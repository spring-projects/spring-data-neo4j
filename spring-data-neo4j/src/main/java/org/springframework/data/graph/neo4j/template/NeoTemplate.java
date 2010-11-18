/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

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
