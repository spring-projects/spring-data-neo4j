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
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.neo4j.helpers.collection.MapUtil.map;

public class RestEntityTests extends RestTestBase  {

    @Test
    public void testSetProperty() {
        Node node = restGraphDatabase.createNode();
        node.setProperty("name", "test");
        Node node2 = restGraphDatabase.getNodeById(node.getId());
        assertEquals("test", node2.getProperty("name"));
    }

    @Test
    public void testSetStringArrayProperty() {
        Node node = restGraphDatabase.createNode();
        node.setProperty("name", new String[]{"test"});
        Node node2 = restGraphDatabase.getNodeById(node.getId());
        Assert.assertArrayEquals( new String[]{"test"}, (String[])node2.getProperty( "name" ) );
    }
    @Test
    public void testSetDoubleArrayProperty() {
        double[] data = {0, 1, 2};
        Node node = restGraphDatabase.createNode();
        node.setProperty("data", data);
        Node node2 = restGraphDatabase.getNodeById(node.getId());
        Assert.assertTrue("same double array",Arrays.equals( data, (double[])node2.getProperty( "data" ) ));
    }

    @Test
    public void testRemoveProperty() {
        Node node = restGraphDatabase.createNode();
        node.setProperty( "name", "test" );
        assertEquals("test", node.getProperty("name"));
        node.removeProperty( "name" );
        assertEquals(false, node.hasProperty("name"));
    }

    @Test(expected = NotFoundException.class)
    public void testRemoveNode() {
        Node node = restGraphDatabase.createNode();
        node.setProperty( "name", "test" );
        final long nodeId = node.getId();
        assertEquals("test", node.getProperty("name"));
        restGraphDatabase.remove(node);
        assertEquals(null, restGraphDatabase.getNodeById(nodeId));
    }

    @Test(expected = NotFoundException.class)
    public void testRemoveRelationship() {
        Node refNode = restGraphDatabase.createNode();
        Node node = restGraphDatabase.createNode();
        Relationship rel = restGraphDatabase.createRelationship(refNode, node, Type.TEST, map("name","test"));
        final long relId = rel.getId();
        assertEquals("test", rel.getProperty("name"));
        restGraphDatabase.remove(rel);
        assertEquals(null, restGraphDatabase.getRelationshipById(relId));
    }


    @Test
    public void testSetPropertyOnRelationship() {
        Node refNode = restGraphDatabase.createNode();
        Node node = restGraphDatabase.createNode();
        Relationship rel = refNode.createRelationshipTo( node, Type.TEST );
        rel.setProperty( "name", "test" );
        assertEquals("test", rel.getProperty("name"));
        Relationship foundRelationship = IsRelationshipToNodeMatcher.relationshipFromTo( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        assertEquals("test", foundRelationship.getProperty("name"));
    }

    @Test
    public void testRemovePropertyOnRelationship() {
        Node refNode = restGraphDatabase.createNode();
        Node node = restGraphDatabase.createNode();
        Relationship rel = refNode.createRelationshipTo( node, Type.TEST );
        rel.setProperty( "name", "test" );
        assertEquals("test", rel.getProperty("name"));
        Relationship foundRelationship = IsRelationshipToNodeMatcher.relationshipFromTo( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        assertEquals("test", foundRelationship.getProperty("name"));
        rel.removeProperty( "name" );
        assertEquals(false, rel.hasProperty("name"));
        Relationship foundRelationship2 = IsRelationshipToNodeMatcher.relationshipFromTo( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        assertEquals(false, foundRelationship2.hasProperty("name"));
    }

}
