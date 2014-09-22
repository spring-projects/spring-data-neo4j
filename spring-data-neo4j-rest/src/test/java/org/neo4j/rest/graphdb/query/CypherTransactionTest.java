package org.neo4j.rest.graphdb.query;

import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.rest.graphdb.RestAPIImpl;
import org.neo4j.rest.graphdb.RestAPIInternal;
import org.neo4j.rest.graphdb.RestTestBase;
import org.neo4j.rest.graphdb.entity.RestEntity;

import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class CypherTransactionTest extends RestTestBase {

    @Test
    public void testSingleSend() throws Exception {
        CypherTransaction transaction = new CypherTransaction(SERVER_ROOT_URI, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("RETURN 42", null);
        assertEquals(asList("42"), result.getColumns());
        Iterator<List<Object>> rows = result.getRows().iterator();
        assertEquals(true,rows.hasNext());
        assertEquals(Arrays.<Object>asList(42), rows.next());
        assertEquals(false,rows.hasNext());
        assertEquals("RETURN 42", result.getStatement().getStatement());
        assertEquals(Collections.<String,Object>emptyMap(), result.getStatement().getParameters());
    }

    @Test
    public void testGraphResult() throws Exception {
        CypherTransaction transaction = new CypherTransaction(SERVER_ROOT_URI, CypherTransaction.ResultType.graph);
        transaction.add("CREATE (n:Person {name:'Graph'}) RETURN n", null);
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(1, commit.size());
        CypherTransaction.Result result = commit.get(0);
        Map nodeMap = (Map) result.getRows().iterator().next().get(0);
        assertEquals("Graph", ((Map)nodeMap.get("properties")).get("name"));
        assertEquals(asList("Person"), nodeMap.get("labels"));
        assertEquals(true, nodeMap.containsKey("id"));

        Node node = getRestGraphDb().getNodeById(Long.parseLong(nodeMap.get("id").toString()));
        assertEquals("Graph",node.getProperty("name"));

    }
    @Test
    public void testRestResult() throws Exception {
        CypherTransaction transaction = new CypherTransaction(SERVER_ROOT_URI, CypherTransaction.ResultType.rest);
        transaction.add("CREATE (n:Person {name:'Rest'}) RETURN n", null);
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(1, commit.size());
        CypherTransaction.Result result = commit.get(0);
        Map nodeMap = (Map) result.getRows().iterator().next().get(0);
        assertEquals("Rest", ((Map)nodeMap.get("data")).get("name"));
        assertEquals(true, nodeMap.containsKey("self"));

        Node node = getRestGraphDb().getNodeById(RestEntity.getEntityId(nodeMap.get("self").toString()));
        assertEquals("Rest",node.getProperty("name"));

    }

    @Test
    public void testCommit() throws Exception {
        CypherTransaction transaction = new CypherTransaction(SERVER_ROOT_URI, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("CREATE (n {name:'John'}) RETURN id(n)", null);
        List<CypherTransaction.Result> commit = transaction.commit();
        assertEquals(0, commit.size());
        Node node = getRestGraphDb().getNodeById(((Number) result.getRows().iterator().next().get(0)).longValue());
        assertEquals("John",node.getProperty("name"));
    }

    @Test(expected = NotFoundException.class)
    public void testRollback() throws Exception {
        CypherTransaction transaction = new CypherTransaction(SERVER_ROOT_URI, CypherTransaction.ResultType.row);
        CypherTransaction.Result result = transaction.send("CREATE (n {name:'John'}) RETURN id(n)", null);
        transaction.rollback();
        RestAPIImpl api = new RestAPIImpl(SERVER_ROOT_URI);
        api.getNodeById(((Number) result.getRows().iterator().next().get(0)).longValue(), RestAPIInternal.Load.ForceFromServer);
    }
}
