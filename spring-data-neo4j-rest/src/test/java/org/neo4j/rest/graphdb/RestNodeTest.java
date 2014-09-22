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

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.neo4j.graphdb.Direction.INCOMING;
import static org.neo4j.graphdb.Direction.OUTGOING;
import static org.neo4j.helpers.collection.IteratorUtil.count;
import static org.neo4j.rest.graphdb.RelationshipHasMatcher.match;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.MatrixDataGraph.RelTypes;

import java.util.List;

public class RestNodeTest extends RestTestBase  {
	
	
	private MatrixDataGraph embeddedMatrixdata;
	private MatrixDataGraph restMatrixData;
	private Node neo;
	

	@Before
	public void createMatrixdata() {
		embeddedMatrixdata = new MatrixDataGraph(getGraphDatabase()).createNodespace();
		restMatrixData = new MatrixDataGraph(getRestGraphDb(),embeddedMatrixdata.getReferenceNode().getId());
		neo = restMatrixData.getNeoNode();
	}
	
	@Test
	public void testGetRelationshipsWithoutDirectionWithoutRelationshipType() {		
		Iterable<Relationship> relationships = neo.getRelationships();		
		assertThat(relationships, match(neo, null));
        assertThat(neo.getDegree(),is(count(relationships)));
	}
	
	@Test
	public void testGetRelationshipsWithIncomingDirectionWithoutRelationshipType() {		
		Iterable<Relationship> relationships = neo.getRelationships(INCOMING);		
		assertThat(relationships, match(neo, INCOMING));
        assertThat(neo.getDegree(INCOMING),is(2));
	}
	
	@Test
	public void testGetRelationshipsWithOutgoingDirectionWithoutRelationshipType() {		
		Iterable<Relationship> relationships = neo.getRelationships(OUTGOING);		
		assertThat(relationships, match(neo, OUTGOING));
        assertThat(neo.getDegree(OUTGOING),is(count(relationships)));
	}
	
	@Test
	public void testGetRelationshipsWithoutDirectionWithSingleRelationshipType() {		
		Iterable<Relationship> relationships = neo.getRelationships(RelTypes.NEO_NODE);		
		assertThat(relationships, match(neo, null, RelTypes.NEO_NODE));
        assertThat(neo.getDegree(RelTypes.NEO_NODE),is(count(relationships)));
	}
	
	
	@Test
	public void testGetRelationshipsWithIncomingDirectionWithSingleRelationshipType() {		
		Iterable<Relationship> relationships = neo.getRelationships(INCOMING, RelTypes.NEO_NODE);		
		assertThat(relationships, match(neo, INCOMING, RelTypes.NEO_NODE));
        assertThat(neo.getDegree(RelTypes.NEO_NODE,INCOMING),is(count(relationships)));
	}
	
	@Test
	public void testGetRelationshipsWithOutgoingDirectionWithSingleRelationshipType() {		
		Iterable<Relationship> relationships = neo.getRelationships(OUTGOING, RelTypes.KNOWS);		
		assertThat(relationships, match(neo, OUTGOING, RelTypes.KNOWS));
        assertThat(neo.getDegree(RelTypes.KNOWS,OUTGOING),is(count(relationships)));
	}
	
	@Test
	public void testGetRelationshipsWithoutDirectionWithMultipleRelationshipTypes() {		
		Iterable<Relationship> relationships = neo.getRelationships(RelTypes.NEO_NODE, RelTypes.HERO);		
		assertThat(relationships, match(neo, null, RelTypes.NEO_NODE, RelTypes.HERO));
	}
	
	@Test
	public void testGetRelationshipsWithIncomingDirectionWithMultipleRelationshipTypes() {		
		Iterable<Relationship> relationships = neo.getRelationships(INCOMING, RelTypes.NEO_NODE, RelTypes.HERO );		
		assertThat(relationships, match(neo, INCOMING, RelTypes.NEO_NODE, RelTypes.HERO));
	}
	
	@Test
	public void testGetRelationshipsWithOutgoingDirectionWithMultipleRelationshipTypes() {		
		Iterable<Relationship> relationships = neo.getRelationships(OUTGOING, RelTypes.KNOWS, RelTypes.FIGHTS );		
		assertThat(relationships, match(neo, OUTGOING, RelTypes.KNOWS, RelTypes.FIGHTS));
	}
	
	@Test
	public void testHasRelationshipsWithoutDirectionWithoutRelationshipType() {		
		boolean hasRelationship = neo.hasRelationship();		
		assertTrue(hasRelationship);
        assertThat(neo.getDegree(),is(5));
	}
	
	@Test
	public void testHasRelationshipsWithIncomingDirectionWithoutRelationshipType() {		
		boolean hasRelationship = neo.hasRelationship(INCOMING);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithOutgoingDirectionWithoutRelationshipType() {		
		boolean hasRelationship = neo.hasRelationship(OUTGOING);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithoutDirectionWithSingleRelationshipType() {		
		boolean hasRelationship = neo.hasRelationship(RelTypes.KNOWS);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithIncomingDirectionWithSingleRelationshipType() {		
		boolean hasRelationship = neo.hasRelationship(INCOMING, RelTypes.HERO);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithIncomingDirectionWithSingleRelationshipTypeParamsReversed() {		
		boolean hasRelationship = neo.hasRelationship(RelTypes.HERO, INCOMING);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithOutgoingDirectionWithSingleRelationshipType() {		
		boolean hasRelationship = neo.hasRelationship(OUTGOING, RelTypes.KNOWS);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithOutgoingDirectionWithSingleRelationshipTypeParamsReversed() {		
		boolean hasRelationship = neo.hasRelationship(RelTypes.KNOWS, OUTGOING);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithoutDirectionWithMultipleRelationshipTypes() {		
		boolean hasRelationship = neo.hasRelationship(RelTypes.KNOWS, RelTypes.HERO);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithIncomingDirectionWithMultipleRelationshipTypes() {		
		boolean hasRelationship = neo.hasRelationship(INCOMING, RelTypes.NEO_NODE, RelTypes.HERO);		
		assertTrue(hasRelationship);		
	}
	
	@Test
	public void testHasRelationshipsWithOutgoingDirectionWithMultipleRelationshipTypes() {		
		boolean hasRelationship = neo.hasRelationship(OUTGOING, RelTypes.KNOWS, RelTypes.FIGHTS);		
		assertTrue(hasRelationship);		
	}

    @Test
    public void testGetNodeLabels() throws Exception {
        Label label = DynamicLabel.label("TestPerson");
        Node node = getRestGraphDb().createNode(label);
        assertTrue(node.hasLabel(label));
        Node node2 = getRestGraphDb().getNodeById(node.getId());
        assertTrue(node2.hasLabel(label));
        Iterable<RelationshipType> types = neo.getRelationshipTypes();
        List<String> expectedTypes = asList(RelTypes.NEO_NODE.name(), RelTypes.HERO.name(), RelTypes.KNOWS.name(), RelTypes.FIGHTS.name());
        int count = 0;
        for (RelationshipType type : types) {
            if (expectedTypes.contains(type.name())) count++;
        }
        assertThat(count,is(expectedTypes.size()));
    }
}
