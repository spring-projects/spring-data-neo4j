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
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.index.IndexHits;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.Collections;

/**
 * Repository like finder for NodeEntities. Provides finder methods for direct access, access via {@link org.springframework.data.graph.core.NodeTypeStrategy}
 * and indexing.
 *
 * @param <T> NodeBacked target of this finder, enables the finder methods to return this concrete type
 */
public class Finder<T extends NodeBacked> {

    /**
     * Target nodebacked type
     */
    private final Class<T> clazz;

    private final GraphDatabaseContext graphDatabaseContext;

    public Finder(final Class<T> clazz, final GraphDatabaseContext graphDatabaseContext) {
        this.clazz = clazz;
        this.graphDatabaseContext = graphDatabaseContext;
    }

    /**
     * @return Number of instances of the target type in the graph.
     */
    public long count() {
        return graphDatabaseContext.count(clazz);
    }

    /**
     * @return lazy Iterable over all instances of the target type.
     */
    public Iterable<T> findAll() {
        return graphDatabaseContext.findAll(clazz);
    }

    /**
     * @param id nodeId
     * @return Node with the given id or null.
     */
    public T findById(final long id) {
        try {
            return graphDatabaseContext.createEntityFromState(graphDatabaseContext.getNodeById(id), clazz);
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Index based single finder.
     * @param property
     * @param value
     * @return Single Node Entity with this property and value
     */
    public T findByPropertyValue(final String property, final Object value) {
        try {
            final Node node = graphDatabaseContext.getSingleIndexedNode(property, value);
            if (node == null) return null;
            return graphDatabaseContext.createEntityFromState(node, clazz);
        } catch (NotFoundException e) {
            return null;
        }

    }

    /**
     * Index based finder.
     * @param property
     * @param value
     * @return Iterable over Node Entities with this property and value
     */
    public Iterable<T> findAllByPropertyValue(final String property, final Object value) {
        try {
            final IndexHits<Node> nodes = graphDatabaseContext.getIndexedNodes(property, value);
            if (nodes == null) return Collections.emptyList();
            return new IterableWrapper<T, Node>(nodes) {
                @Override
                protected T underlyingObjectToObject(final Node node) {
                    return graphDatabaseContext.createEntityFromState(node, clazz);
                }
            };
        } catch (NotFoundException e) {
            return null;
        }
    }

    /**
     * Traversal based finder that returns a lazy Iterable over the traversal results
     * @param startNode the node to start the traversal from
     * @param traversalDescription
     * @param <N> Start node entity type
     * @return Iterable over traversal result
     */
    public <N extends NodeBacked> Iterable<T> findAllByTraversal(final N startNode, final TraversalDescription traversalDescription) {
        return (Iterable<T>) startNode.find(clazz, traversalDescription);
    }
}

