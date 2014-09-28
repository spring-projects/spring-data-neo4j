/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.rest.support;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.RelationshipIndex;

import java.util.Arrays;

public class RestIndexTests extends RestTestBase {

    private static final String NODE_INDEX_NAME = "NODE_INDEX";
    private static final String REL_INDEX_NAME = "REL_INDEX";

    @Test
    public void testAddToNodeIndex() {
        nodeIndex().add(node(), "name", "test");
        IndexHits<Node> hits = nodeIndex().get("name", "test");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
        hits.close();
    }

    @Test
    public void testAdvancedQuery() {
        nodeIndex().add(node(), "name", "test");
        IndexHits<Node> hits = nodeIndex().query("name", "tes*");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
        hits.close();
    }

        @Test
    public void testRangeQuery() {
        nodeIndex().add(node(), "age", 35);
        IndexHits<Node> hits = nodeIndex().query("age", "age:[30 TO 40]");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
        hits.close();
    }

    @Test
    public void testNotFoundInNodeIndex() {
        IndexHits<Node> hits = nodeIndex().get("foo", "bar");
        Assert.assertEquals("no index results", false, hits.hasNext());
        hits.close();
    }

    @Test
    public void testAddToRelationshipIndex() {
        final long value = System.currentTimeMillis();
        relationshipIndex().add(relationship(), "name", value);
        IndexHits<Relationship> hits = relationshipIndex().get("name", value);
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(relationship(), hits.next());
        hits.close();
    }

    @Test
    public void testNotFoundInRelationshipIndex() {
        IndexHits<Relationship> hits = relationshipIndex().get("foo", "bar");
        Assert.assertEquals("no index results", false, hits.hasNext());
        hits.close();
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
        hits.close();
        hitsAfterRemove.close();
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
        hits.close();
        hitsAfterRemove.close();
    }

    private Index<Node> nodeIndex() {
        return index().forNodes(NODE_INDEX_NAME);
    }

    private RelationshipIndex relationshipIndex() {
        return index().forRelationships(REL_INDEX_NAME);
    }

    @Test
    public void testNodeIndexIsListed() {
        nodeIndex().add(node(), "name", "test");
        Assert.assertTrue("node index name listed", Arrays.asList(index().nodeIndexNames()).contains(NODE_INDEX_NAME));
    }

    @Test
    public void testRelationshipIndexIsListed() {
        relationshipIndex().add(relationship(), "name", "test");
        Assert.assertTrue("relationship index name listed", Arrays.asList(index().relationshipIndexNames()).contains(REL_INDEX_NAME));
    }

}
