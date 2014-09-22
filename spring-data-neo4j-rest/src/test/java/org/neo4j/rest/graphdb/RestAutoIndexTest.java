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

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.*;

import java.net.URISyntaxException;

import static org.junit.Assert.*;

public class RestAutoIndexTest extends RestTestBase {

    @Test
    public void testEnableDisableAutoIndexerNode() {
        AutoIndexer<Node> indexer = getRestGraphDb().index().getNodeAutoIndexer();
        testEnableDisableAutoIndexer(indexer);
    }

    @Test
    public void testEnableDisableAutoIndexerRelationship() {
        RelationshipAutoIndexer indexer = getRestGraphDb().index().getRelationshipAutoIndexer();
        testEnableDisableAutoIndexer(indexer);
    }

    @Test
    public void testAddRemoveAutoIndexerPropertiesOnNodes() {
        AutoIndexer<Node> indexer = getRestGraphDb().index().getNodeAutoIndexer();
        testAddRemoveAutoIndexerProperties(indexer);
    }

    @Test
    public void testAddRemoveAutoIndexerPropertiesOnRelationships() {
        RelationshipAutoIndexer indexer = getRestGraphDb().index().getRelationshipAutoIndexer();
        testAddRemoveAutoIndexerProperties(indexer);
    }

    @Test
    public void testGetAutoIndexOnNodes() {
        ReadableIndex<Node> autoIndex = getRestGraphDb().index().getNodeAutoIndexer().getAutoIndex();
        assertNotNull(autoIndex);
    }

    @Test
    public void testGetAutoIndexOnRelationships() {
        ReadableRelationshipIndex autoIndex = getRestGraphDb().index().getRelationshipAutoIndexer().getAutoIndex();
        assertNotNull(autoIndex);
    }

    @Test
    public void testAutoIndexingByCheckingIndexData() {
        IndexManager indexManager = getRestGraphDb().index();

        AutoIndexer<Node> nodeAutoIndexer = indexManager.getNodeAutoIndexer();
        RelationshipAutoIndexer relationshipAutoIndex = indexManager.getRelationshipAutoIndexer();

        // setup auto indexing
        nodeAutoIndexer.startAutoIndexingProperty("nodeProperty");
        nodeAutoIndexer.setEnabled(true);
        relationshipAutoIndex.startAutoIndexingProperty("relationshipProperty");
        relationshipAutoIndex.setEnabled(true);

        // create two connected nodes
        Node startNode = getRestGraphDb().createNode();
        Node endNode = getRestGraphDb().createNode();
        startNode.setProperty("nodeProperty", "startNode");
        endNode.setProperty("nodeProperty", "endNode");
        Relationship relationship = startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName("sample"));
        relationship.setProperty("relationshipProperty", "sample");

        // check index data
        ReadableIndex<Node> nodeAutoIndex = nodeAutoIndexer.getAutoIndex();
        IndexHits<Node> nodeHits = nodeAutoIndex.get("nodeProperty", "startNode");
        Node nodeByIndex = nodeHits.getSingle();
        assertEquals(startNode, nodeByIndex);

        nodeHits = nodeAutoIndex.get("nodeProperty", "endNode");
        nodeByIndex = nodeHits.getSingle();
        assertEquals(endNode, nodeByIndex);

        nodeHits = nodeAutoIndex.get("nodeProperty", "nonExistingValue");
        assertEquals(0, nodeHits.size());

        IndexHits<Relationship> relationshipHits = relationshipAutoIndex.getAutoIndex().get("relationshipProperty", "sample");
        Relationship relationshipByIndex = relationshipHits.getSingle();
        assertEquals(relationship, relationshipByIndex);
    }

    private void testAddRemoveAutoIndexerProperties(AutoIndexer<? extends PropertyContainer> indexer) {
        assertTrue(indexer.getAutoIndexedProperties().isEmpty());

        indexer.startAutoIndexingProperty("property1");
        assertTrue(indexer.getAutoIndexedProperties().size()==1);
        assertTrue(indexer.getAutoIndexedProperties().contains("property1"));

        indexer.startAutoIndexingProperty("property2");
        assertTrue(indexer.getAutoIndexedProperties().size() == 2);
        assertTrue(indexer.getAutoIndexedProperties().contains("property2"));

        indexer.stopAutoIndexingProperty("property2");
        assertTrue(indexer.getAutoIndexedProperties().size() == 1);
        assertFalse(indexer.getAutoIndexedProperties().contains("property2"));

        indexer.stopAutoIndexingProperty("property1");
        assertTrue(indexer.getAutoIndexedProperties().isEmpty());

        indexer.stopAutoIndexingProperty("propertyUnknown");
        assertTrue(indexer.getAutoIndexedProperties().isEmpty());

    }

    private void testEnableDisableAutoIndexer(AutoIndexer<? extends PropertyContainer> indexer) {
        assertFalse("indexer is not enabled by default", indexer.isEnabled());
        indexer.setEnabled(true);
        assertTrue("indexer was enabled",indexer.isEnabled());
        indexer.setEnabled(false);
        assertFalse("indexer was disabled", indexer.isEnabled());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        getGraphDatabase().index().getNodeAutoIndexer().setEnabled(false);
        getGraphDatabase().index().getRelationshipAutoIndexer().setEnabled(false);
    }
}
