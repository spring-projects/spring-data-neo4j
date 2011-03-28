package org.neo4j.rest.graphdb;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;

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
