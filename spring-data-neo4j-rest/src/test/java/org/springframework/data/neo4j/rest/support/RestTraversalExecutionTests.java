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
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.rest.graphdb.traversal.RestTraversal;

public class RestTraversalExecutionTests extends RestTestBase {
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
