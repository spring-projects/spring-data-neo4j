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

import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.kernel.Uniqueness;
import org.neo4j.rest.graphdb.traversal.RestTraversal;
import org.neo4j.rest.graphdb.traversal.RestTraversalDescription;

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
        Map pruneEvaluator= (Map) getPostData("prune_evaluator");       
        Assert.assertEquals("javascript", pruneEvaluator.get("language"));
        Assert.assertEquals("return true;", pruneEvaluator.get("body"));
    }

    @Test
    public void testFilterScript() throws Exception {
        traversalDescription.filter(RestTraversalDescription.ScriptLanguage.JAVASCRIPT, "return true;");
        Map pruneEvaluator= (Map) getPostData("return_filter");
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
