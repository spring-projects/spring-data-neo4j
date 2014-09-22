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

import java.util.Collection;
import java.util.Date;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.util.TestHelper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItems;

public class RestGraphDbTest extends RestTestBase {

    @Test
    public void testGetRefNode() {
        Node refNode = node();
        Node nodeById = getRestGraphDb().getNodeById( refNode.getId() );
        Assert.assertEquals( refNode, nodeById );
    }

    @Test
    public void testCreateNode() {
        Node node = getRestGraphDb().createNode();
        Assert.assertEquals( node, getRestGraphDb().getNodeById( node.getId() ) );
    }

    @Test
    public void testCreateRelationship() {
        Node refNode = node();
        Node node = getRestGraphDb().createNode();
        Relationship rel = refNode.createRelationshipTo( node, Type.TEST );
        Relationship foundRelationship = TestHelper.firstRelationshipBetween( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        Assert.assertNotNull( "found relationship", foundRelationship );
        Assert.assertEquals( "same relationship", rel, foundRelationship );
        Assert.assertThat( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Direction.OUTGOING ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Direction.BOTH ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Type.TEST ), new IsRelationshipToNodeMatcher( refNode, node ) );
    }

    @Test
    public void testBasic() {
        final GraphDatabaseService gdb = getRestGraphDb();
        Node refNode = node();
        Node node = gdb.createNode();
        final RelationshipType TEST = DynamicRelationshipType.withName("TEST");
        Relationship rel = refNode.createRelationshipTo( node,
                TEST);
        rel.setProperty( "date", new Date().getTime() );
        node.setProperty( "name", "Mattias test" );
        refNode.createRelationshipTo( node,
                TEST);

        for ( Relationship relationship : refNode.getRelationships() ) {
            System.out.println( "rel prop:" + relationship.getProperty( "date", null ) );
            Node endNode = relationship.getEndNode();
            System.out.println( "node prop:" + endNode.getProperty( "name", null ) );
        }
        assertThat(gdb.getAllNodes(),hasItems(refNode, node));
        boolean found = false;
        for (RelationshipType type : gdb.getRelationshipTypes()) {
            found |= TEST.name().equals(type.name());
        }
        assertEquals("rel-type TEST found",true,found);
    }

    @Test
    public void testCreateNodeWithLabels() {
        Label label1 = DynamicLabel.label("FOO");
        Label label2 = DynamicLabel.label("BAR");
        Node node = getRestGraphDb().createNode(label1, label2);
        Collection<Label> labels = IteratorUtil.asCollection(node.getLabels());
        assertEquals(2,labels.size());
        for (Label label : labels) {
            assertTrue(label.name().equals(label1.name()) || label.name().equals(label2.name()));
        }
    }

    @Test
    public void testGetNodesByLabelAndProperty() throws Exception {
        Label label1 = DynamicLabel.label("FOO");
        Label label2 = DynamicLabel.label("BAR");
        GraphDatabaseService db = getRestGraphDb();
        Node node = db.createNode(label1, label2);
        node.setProperty("name","foo bar");
        node.setProperty("age",42);
        Collection<Node> nodes = IteratorUtil.asCollection(db.findNodesByLabelAndProperty(label1, "name", "foo bar"));
        assertEquals(1,nodes.size());
        assertEquals(node,nodes.iterator().next());

        nodes = IteratorUtil.asCollection(db.findNodesByLabelAndProperty(label2, "age", 42));
        assertEquals(1,nodes.size());
        assertEquals(node,nodes.iterator().next());

        nodes = IteratorUtil.asCollection(db.findNodesByLabelAndProperty(label2, "age", 43));
        assertEquals(0,nodes.size());
    }
}
