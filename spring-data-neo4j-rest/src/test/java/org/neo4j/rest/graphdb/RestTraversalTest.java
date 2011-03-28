package org.neo4j.rest.graphdb;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Uniqueness;

import java.util.Map;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public class RestTraversalTest {
    private RestTraversal traversalDescription;

    @Before
    public void setUp() throws Exception {
        traversalDescription = (RestTraversal) RestTraversal.description();
    }

    @Test
    public void testUniqueness() throws Exception {
        traversalDescription.uniqueness(Uniqueness.NODE_PATH);
        Assert.assertEquals("node path", getPostData("uniqueness"));
    }

    @Test
    public void testUniquenessWithValue() throws Exception {
        traversalDescription.uniqueness(Uniqueness.NODE_PATH,"test");
        String param = "uniqueness";
        final Map uniquenessMap = (Map) getPostData(param);
        Assert.assertEquals("node path", uniquenessMap.get("name"));
        Assert.assertEquals("test", uniquenessMap.get("value"));
    }

    private Object getPostData(String param) {
        return traversalDescription.getPostData().get(param);
    }

    @Test
    public void testPruneScript() throws Exception {
        traversalDescription.prune(RestTraversalDescription.ScriptLanguage.JAVASCRIPT, "return true;");
        Map pruneEvaluator= (Map) getPostData("prune evaluator");
        Assert.assertEquals("javascript", pruneEvaluator.get("language"));
        Assert.assertEquals("return true;", pruneEvaluator.get("body"));
    }

    @Test
    public void testFilterScript() throws Exception {
        traversalDescription.filter(RestTraversalDescription.ScriptLanguage.JAVASCRIPT, "return true;");
        Map pruneEvaluator= (Map) getPostData("return filter");
        Assert.assertEquals("javascript", pruneEvaluator.get("language"));
        Assert.assertEquals("return true;", pruneEvaluator.get("body"));
    }

    @Test
    public void testEvaluator() throws Exception {

    }

    @Test
    public void testPrune() throws Exception {

    }

    @Test
    public void testFilter() throws Exception {

    }

    @Test
    public void testMaxDepth() throws Exception {

    }

    @Test
    public void testOrder() throws Exception {

    }

    @Test
    public void testDepthFirst() throws Exception {

    }

    @Test
    public void testBreadthFirst() throws Exception {

    }

    @Test
    public void testRelationships() throws Exception {

    }

    @Test
    public void testRelationshipsAndDirection() throws Exception {

    }

    @Test
    public void testExpand() throws Exception {

    }
    @Test
    public void testComplexTraversal() throws Exception {

    }
}
