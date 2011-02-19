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

package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IteratorWrapper;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.graph.UncategorizedGraphStoreException;

import java.util.Iterator;
import java.util.List;

public class Neo4jTemplate {
    private final GraphDatabaseService graphDatabaseService;

    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        if (graphDatabaseService == null)
            throw new IllegalArgumentException("GraphDatabaseService must not be null");
        this.graphDatabaseService = graphDatabaseService;
    }

    public void doInTransaction(final TransactionGraphCallback callback) {
        if (callback == null)
            throw new IllegalArgumentException("Callback must not be null");
        execute(callback);
    }


    public void execute(final GraphCallback callback) {
        if (callback == null)
            throw new IllegalArgumentException("Callback must not be null");
        try {
            callback.doWithGraph(graphDatabaseService);
        } catch (Exception e) {
            throw new UncategorizedGraphStoreException("Error executing callback", e);
            // todo exception translation
        }
    }

    public Node getReferenceNode() {
        return graphDatabaseService.getReferenceNode();
    }
    public Node createNode() {
        return graphDatabaseService.createNode();
    }
    public Node getNode(long id) {
        return graphDatabaseService.getNodeById(id);
    }

    public void index(PropertyContainer primitive, String indexName, String field, Object value) {
        if (primitive instanceof Node)
            graphDatabaseService.index().forNodes(indexName == null ? "node" : indexName).add((Node) primitive, field,value);
        if (primitive instanceof Relationship)
            graphDatabaseService.index().forRelationships(indexName == null ? "relationship" : indexName).add((Relationship) primitive, field,value);
        throw new IllegalArgumentException("Supplied graph primitive is null");
    }

    public void index(PropertyContainer primitive, String field, Object value) {
        index(primitive,null,field,value);
    }

    public <T> Iterator<T> traverseNodes(Node startNode, TraversalDescription traversal, final Converter<Node, T> converter) {
        return new IteratorWrapper<T, Node>(traversal.traverse(startNode).nodes().iterator()) {
            @Override
            protected T underlyingObjectToObject(Node node) {
                return converter.convert(node);
            }
        };
    }
}
