package org.springframework.data.neo4j.support.schema;

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.EndResult;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.query.QueryEngine;

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

    public SchemaIndexProvider(GraphDatabase gd) {
        this.gd = gd;
        cypher = gd.queryEngineFor(QueryType.Cypher);
    }

    public void createIndex(Neo4jPersistentProperty property) {
        IndexInfo indexInfo = property.getIndexInfo();
        String label = getLabelToIndexAgainst(property);
        String prop = property.getNeo4jPropertyName();
        String query = indexQuery(label, prop, indexInfo.isUnique());
        createIndexInSeparateTx(label,prop,query);
    }


    public String getLabelToIndexAgainst(Neo4jPersistentProperty property) {
        IndexInfo indexInfo = property.getIndexInfo();
        if (property.getOwner().getEntityType() == null) {
            //throw new RuntimeException("Need the entity to know what label(s) to index against");
            logger.info("TODO - This may well cause problems. EntityType is required at this" +
                        "       stage to ensure the correct label is obtained and used, however " +
                        "       it is not available() defaulting to simply name for now ....  ");
            return indexInfo.getIndexName();
        }
        return (String)property.getOwner().getEntityType().getAlias();
    }

    public void createIndexInSeparateTx(final String label,final String prop, final String query) {
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
        ExecutorService pool = Executors.newFixedThreadPool(1);

        try {
            pool.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    try (Transaction tx = gd.beginTx()) {
                        cypher.query(query, null);
                        tx.success();
                        //logger.info("Created index via cypher - " + query);
                    }
                    return true;
                }
            }).get(2,TimeUnit.SECONDS);
            pool.shutdown();
        } catch (TimeoutException e) {
            throw new MappingException(format(
                    "Timeout occured trying to create schema index %s on against label %s: " +
                    "This may well be an indicator that another thread (the one which just " +
                    "initiated this update), has probably got a lock of " +
                    "this node and this timeout is because its in a deadlock situation and" +
                    " cant acquire it - investigation required", prop, label),e);
        } catch (Exception e) {
            throw new MappingException(format("Unable to create schema index %s on against label %s", prop, label),e);
        }
    }

    public <T> EndResult<T> findAll(Neo4jPersistentEntity entity) {
        String label = entity.getTypeAlias().toString();
        String query = findByLabelQuery(label);
        return cypher.query(query, null).<T>to(entity.getType());
    }

    public <T> EndResult<T> findAll(Neo4jPersistentProperty property, Object value) {
        IndexInfo indexInfo = property.getIndexInfo();
        String label = indexInfo.getIndexName();
        String prop = property.getNeo4jPropertyName();
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
}
