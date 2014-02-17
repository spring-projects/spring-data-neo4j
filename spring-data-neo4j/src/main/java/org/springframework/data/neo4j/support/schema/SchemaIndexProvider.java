package org.springframework.data.neo4j.support.schema;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Queue;
import java.util.concurrent.*;

import static java.lang.String.format;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 20.01.14
 */
public class SchemaIndexProvider {
    private final GraphDatabase gd;
    private final QueryEngine<Object> cypher;

    private static final Logger logger = LoggerFactory.getLogger(SchemaIndexProvider.class);
    private final IndexCreator indexCreator;

    public SchemaIndexProvider(GraphDatabase gd) {
        this.gd = gd;
        if (this.gd instanceof DelegatingGraphDatabase) {
            TransactionalHandler handler = new TransactionalHandler();
            ((DelegatingGraphDatabase)this.gd).getGraphDatabaseService().registerTransactionEventHandler(handler);
            indexCreator = handler;
        } else {
            indexCreator = new SeparateThreadIndexCreator();
        }
        cypher = gd.queryEngineFor(QueryType.Cypher);
    }

    public void createIndex(Neo4jPersistentProperty property) {
        indexCreator.deferCreateIndex(property);
    }
    public void doCreateIndex(Neo4jPersistentProperty property) {
        String label = getLabel(property);
        String prop = getName(property);
        String query = indexQuery(label, prop, property.getIndexInfo().isUnique());
        cypher.query(query,null);
    }

    private String getName(Neo4jPersistentProperty property) {
        return property.getNeo4jPropertyName();
    }

    private String getLabel(Neo4jPersistentProperty property) {
        return property.getIndexInfo().getIndexName();
    }

    public <T> EndResult<T> findAll(Neo4jPersistentEntity entity) {
        String label = entity.getTypeAlias().toString();
        String query = findByLabelQuery(label);
        return cypher.query(query, null).<T>to(entity.getType());
    }

    public <T> EndResult<T> findAll(Neo4jPersistentProperty property, Object value) {
        IndexInfo indexInfo = property.getIndexInfo();
        String label = indexInfo.getIndexName();
        String prop = getName(property);
        String query = findByLabelAndPropertyQuery(label, prop);
        return cypher.query(query, map("value", value)).<T>to((Class<T>)property.getOwner().getType());
    }

    private String findByLabelQuery(String label) {
        return "MATCH (n:`"+label+"`) RETURN n";
    }

    private String findByLabelAndPropertyQuery(String label, String prop) {
        return "MATCH (n:`"+label+"` {`"+prop+"`:{value}}) RETURN n";
    }

    private String indexQuery(String label, String prop, boolean unique) {
        if (unique) {
            return  "CREATE CONSTRAINT ON (n:`"+ label +"`) ASSERT n.`"+ prop +"` IS UNIQUE";
        }
        return "CREATE INDEX ON :`"+ label +"`(`"+ prop +"`)";
    }

    interface IndexCreator {
        void deferCreateIndex(Neo4jPersistentProperty property);
    }

    private class TransactionalHandler extends TransactionEventHandler.Adapter<Object> implements IndexCreator {
        Queue<Neo4jPersistentProperty> indexesToBeCreated=new ArrayBlockingQueue<>(10);

        @Override
        public void afterCommit(TransactionData data, Object state) {
            runAfterTransaction();
        }

        private void runAfterTransaction() {
            if (gd.transactionIsRunning() || indexesToBeCreated.isEmpty()) return;
            Neo4jPersistentProperty property = indexesToBeCreated.poll();
            doCreateIndex(property);
        }

        @Override
        public void afterRollback(TransactionData data, Object state) {
            runAfterTransaction();
        }

        @Override
        public void deferCreateIndex(Neo4jPersistentProperty property) {
            indexesToBeCreated.add(property);
        }
    }

    private class SeparateThreadIndexCreator implements IndexCreator {
        private final ExecutorService pool = Executors.newFixedThreadPool(1);
        @Override
        public void deferCreateIndex(final Neo4jPersistentProperty property) {
        /* 1) NW-ISSUE01
              If we don't do this in a separate tx we get the following
              error depending on certain circumstances .... :
              "org.neo4j.cypher.CypherExecutionException: Cannot perform
              schema updates in a transaction that has performed data updates."

              HOWEVER, even doing this does not necessarily work in all cases as
              often it appears that there have been some previous updates in the
              original calling thread, which itself took out some locks and then
              essentially blocks this code from ever completing ... As a temp
              measure introducing a timeout to catch this case rather than letting
              it just hang forever (see LabelBasedIndexedPropertyHangingEntityTests)

              TODO: Look at an alternative approach / way to ensure these schema
                    updates are the first things done - this is not very efficient
                    as it stands anyway
             */

            try {
                pool.submit(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        doCreateIndex(property);
                        return true;
                    }
                }).get(2, TimeUnit.SECONDS);
                pool.shutdown();
            } catch (TimeoutException e) {
                throw new MappingException(format(
                        "Timeour occured trying to create schema index %s on against label %s: " +
                                "This may well be an indicator that another thread (the one which just " +
                                "initiated this update), has probably got a lock of " +
                                "this node and this timeout is because its in a deadlock situation and" +
                                " cant acquire it - investigation required", getName(property), getLabel(property)),e);
            } catch (Exception e) {
                throw new MappingException(format("Unable to create schema index %s on against label %s", getName(property), getLabel(property)),e);
            }
        }
    }
}
