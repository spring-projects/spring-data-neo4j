/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.index.lucene.QueryContext;

import static org.junit.Assert.assertEquals;

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
    public void testUseCriticalCharactersInKeyAndValue() {
        nodeIndex().add(node(), "na#me", "te?t");
        IndexHits<Node> hits = nodeIndex().get("na#me", "te?t");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
    }

    @Test
    public void testPutNodeIfAbsentIndex() {
        final Node node = nodeIndex().putIfAbsent(node(), "name", "test");
        Assert.assertEquals(node(), node);
        IndexHits<Node> hits = nodeIndex().get("name", "test");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
    }
    @Test
    public void testPutNodeIfAbsentWithExistingNodeIndex() {
        nodeIndex().add(node(), "name", "test");
        final Node node = nodeIndex().putIfAbsent(node(), "name", "test");
        Assert.assertEquals(node(), node);
        IndexHits<Node> hits = nodeIndex().get("name", "test");
        Assert.assertEquals("index results", true, hits.hasNext());
        Assert.assertEquals(node(), hits.next());
    }

    @Test
    public void testStarQuery() {
        Node node = node();
        nodeIndex().add(node, "name", "test");
        Node res = nodeIndex().query("*:*").getSingle();
        assertEquals(node,res);
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
    public void testDeleteKeyValueFromNodeIndex() {
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
    public void testDeleteKeyFromNodeIndex() {
        String value = String.valueOf(System.currentTimeMillis());
        nodeIndex().add(node(), "time", value);
        IndexHits<Node> hits = nodeIndex().get("time", value);
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
        nodeIndex().remove(node(), "time");
        IndexHits<Node> hitsAfterRemove = nodeIndex().get("time", value);
        Assert.assertEquals("not found in index results", false, hitsAfterRemove.hasNext());
    }
    @Test
    public void testDeleteNodeFromNodeIndex() {
        String value = String.valueOf(System.currentTimeMillis());
        nodeIndex().add(node(), "time", value);
        IndexHits<Node> hits = nodeIndex().get("time", value);
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
        nodeIndex().remove(node());
        IndexHits<Node> hitsAfterRemove = nodeIndex().get("time", value);
        Assert.assertEquals("not found in index results", false, hitsAfterRemove.hasNext());
    }
    @Test
    public void testDeleteIndex() {
        final String indexName = nodeIndex().getName();
        nodeIndex().delete();
        final List<String> indexNames = Arrays.asList(getRestGraphDb().index().nodeIndexNames());
        Assert.assertEquals("removed index name",false,indexNames.contains(indexName));
    }
    @Test
    public void testCreateFulltextIndex() {
        Map<String,String> config=new HashMap<String, String>();
        config.put("provider", "lucene");
        config.put("type","fulltext");
        final IndexManager indexManager = getRestGraphDb().index();
        final Index<Node> index = indexManager.forNodes("fulltext", config);
        final Map<String, String> config2 = indexManager.getConfiguration(index);
        Assert.assertEquals("provider", config.get("provider"), config2.get("provider"));
        Assert.assertEquals("type", config.get("type"), config2.get("type"));
    }

    @Test
    public void testQueryFulltextIndexWithKey() {
        Map<String,String> config=new HashMap<String, String>();
        config.put("provider","lucene");
        config.put("type","fulltext");
        final Index<Node> index = getRestGraphDb().index().forNodes("text-index", config);
        index.add(node(),"text","any text");
        final IndexHits<Node> hits = index.query("text", "any t*");
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
    }
    @Test
    public void testQueryFulltextIndexWithOutKey() {
        Map<String,String> config=new HashMap<String, String>();
        config.put("provider","lucene");
        config.put("type","fulltext");
        final Index<Node> index = getRestGraphDb().index().forNodes("text-index", config);
        index.add(node(),"text","any text");
        final IndexHits<Node> hits = index.query("text:any t*");
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
    }

    @Test
    public void testQueryFulltextIndexWithLuceneQueryTerm() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("provider", "lucene");
        config.put("type", "fulltext");
        final Index<Node> index = getRestGraphDb().index().forNodes("text-index", config);
        index.add(node(), "text", "any text");
        TermQuery luceneQuery = new TermQuery(new Term("text", "any t*"));
        // TODO this works only because the toString implementation renders a complete query -> dangerous assumption
        final IndexHits<Node> hits = index.query(luceneQuery);
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
    }

    @Test
    public void testQueryFulltextIndexWithQueryContext() {
        Map<String, String> config = new HashMap<String, String>();
        config.put("provider", "lucene");
        config.put("type", "fulltext");
        final Index<Node> index = getRestGraphDb().index().forNodes("text-index", config);
        index.add(node(), "text", "any text");
        QueryContext ctx = new QueryContext("text:any t*");
        final IndexHits<Node> hits = index.query(ctx);
        Assert.assertEquals("found in index results", true, hits.hasNext());
        Assert.assertEquals("found in index results", node(), hits.next());
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
        return getRestGraphDb().index().forNodes(NODE_INDEX_NAME);
    }

    private RelationshipIndex relationshipIndex() {
        return getRestGraphDb().index().forRelationships(REL_INDEX_NAME);
    }

    @Test
    public void testNodeIndexIsListed() {
        nodeIndex().add(node(), "name", "test");
        Assert.assertTrue("node index name listed", Arrays.asList(getRestGraphDb().index().nodeIndexNames()).contains(NODE_INDEX_NAME));
    }

    @Test
    public void testRelationshipIndexIsListed() {
        relationshipIndex().add(relationship(), "name", "test");
        Assert.assertTrue("relationship index name listed", Arrays.asList(getRestGraphDb().index().relationshipIndexNames()).contains(REL_INDEX_NAME));
    }

	
}
