/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.finder;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

public class NodeFinder<T extends GraphBacked<Node>> extends AbstractFinder<Node, T> {

    public NodeFinder(final Class<T> clazz, final GraphDatabaseContext graphDatabaseContext) {
        super(graphDatabaseContext, clazz);
    }

    @Override
    protected Node getById(long id) {
        return graphDatabaseContext.getNodeById(id);
    }

    @Override
    protected Index<Node> getIndex(String indexName) {
        return graphDatabaseContext.getNodeIndex(indexName);
    }

    @Override
    public <N extends NodeBacked> Iterable<T> findAllByTraversal(final N startNode, final TraversalDescription traversalDescription) {
        return (Iterable<T>) startNode.findAllByTraversal((Class<? extends NodeBacked>) clazz, traversalDescription);
    }
}

