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

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.rest.graphdb.traversal.RestTraversal;

public class RestTraversalExecutionTest extends RestTestBase {
    @Test
    public void testTraverseToNeighbour() {
        final Relationship rel = relationship();
        final TraversalDescription traversalDescription = RestTraversal.description().maxDepth(1).breadthFirst();
        System.out.println("traversalDescription = " + traversalDescription);
        final Traverser traverser = traversalDescription.traverse(rel.getStartNode());
        final Iterable<Node> nodes = traverser.nodes();
        Assert.assertEquals(rel.getEndNode(), nodes.iterator().next());
    }

}
