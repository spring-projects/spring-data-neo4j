package org.neo4j.rest.graphdb;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.query.RestCypherQueryEngine;
import org.neo4j.rest.graphdb.util.Config;
import org.neo4j.rest.graphdb.util.QueryResult;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author Michael Hunger @since 27.10.13
 */
public class SimpleTransactionTest extends RestTestBase {

    @Test
    public void testQueryWithinTransaction() throws Exception {
        RestGraphDatabase db = (RestGraphDatabase) getRestGraphDb();
        RestCypherQueryEngine cypher = new RestCypherQueryEngine(db.getRestAPI());
        Transaction tx = db.beginTx();
        QueryResult<Map<String,Object>> result = cypher.query("CREATE (person1 { personId: {id}, started: {started} }) return person1",
                map("id", 1, "started", System.currentTimeMillis()));
        try {
            result.to(Node.class).singleOrNull();
        } catch(IllegalStateException ise) { assertEquals(true, ise.getMessage().contains("finish the transaction")); }
        tx.success();
        tx.close();
        Node node = result.to(Node.class).singleOrNull();
        assertNotNull(node);
        assertEquals(1,node.getProperty("personId"));
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"true");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        System.clearProperty(Config.CONFIG_BATCH_TRANSACTION);
        super.tearDown();
    }
}
