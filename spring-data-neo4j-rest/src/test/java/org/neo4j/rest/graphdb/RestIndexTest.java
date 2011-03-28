package org.neo4j.rest.graphdb;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;

import java.util.Arrays;
import java.util.Iterator;

public class RestIndexTest extends RestTestBase {

    private static final String NODE_INDEX_NAME = "NODE_INDEX";
    private static final String REL_INDEX_NAME = "REL_INDEX";

    @Test
    public void testAddToNodeIndex() {
        nodeIndex().add(node(), "name", "test");
        IndexHits<Node> hits = nodeIndex().get("name", "test");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
    }

    @Test
    public void testNotFoundInNodeIndex() {
        IndexHits<Node> hits = nodeIndex().get("foo", "bar");
        Assert.assertEquals("no index results", false, hits.hasNext());
    }

    @Test
    public void testAddToRelationshipIndex() {
        final long value = System.currentTimeMillis();
        relationshipIndex().add(relationship(), "name", value);
        IndexHits<Relationship> hits = relationshipIndex().get("name", value);
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(relationship(), hits.next());
    }

    @Test
    public void testNotFoundInRelationshipIndex() {
        IndexHits<Relationship> hits = relationshipIndex().get("foo", "bar");
        Assert.assertEquals("no index results", false, hits.hasNext());
    }

    @Test
    public void testDeleteFromNodeIndex() {
        String value = String.valueOf(System.currentTimeMillis());
        nodeIndex().add(node(), "time", value);
        IndexHits<Node> hits = nodeIndex().get("time", value);
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
        nodeIndex().remove(node(), "time", value);
        IndexHits<Node> hitsAfterRemove = nodeIndex().get("time", value);
        Assert.assertEquals("not found in index results", false, hitsAfterRemove.hasNext());
    }

    @Test
    public void testDeleteFromRelationshipIndex() {
        String value = String.valueOf(System.currentTimeMillis());
        relationshipIndex().add(relationship(), "time", value);
        IndexHits<Relationship> hits = relationshipIndex().get("time", value);
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", relationship(), hits.next());
        relationshipIndex().remove(relationship(), "time", value);
        IndexHits<Relationship> hitsAfterRemove = relationshipIndex().get("time", value);
        Assert.assertEquals("not found in index results", false, hitsAfterRemove.hasNext());
    }

    private Index<Node> nodeIndex() {
        return graphDb.index().forNodes(NODE_INDEX_NAME);
    }

    private RelationshipIndex relationshipIndex() {
        return graphDb.index().forRelationships(REL_INDEX_NAME);
    }

    @Test
    public void testNodeIndexIsListed() {
        nodeIndex().add(node(), "name", "test");
        Assert.assertTrue("node index name listed", Arrays.asList(graphDb.index().nodeIndexNames()).contains(NODE_INDEX_NAME));
    }

    @Test
    public void testRelationshipIndexIsListed() {
        relationshipIndex().add(relationship(), "name", "test");
        Assert.assertTrue("relationship index name listed", Arrays.asList(graphDb.index().relationshipIndexNames()).contains(REL_INDEX_NAME));
    }

}
