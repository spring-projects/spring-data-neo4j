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

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.UncategorizedGraphStoreException;

import java.util.Map;

public class Neo4jTemplate implements Neo4jOperations {

    private final boolean useExplicitTransactions;

    private final GraphDatabaseService graphDatabaseService;

    private final Neo4jExceptionTranslator exceptionTranslator = new Neo4jExceptionTranslator();
    private final IndexManager index;

    private static void notNull(Object... pairs) {
        assert pairs.length % 2 == 0 : "wrong number of pairs to check";
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == null) {
                throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + pairs[i + 1] + " is required; it must not be null");
            }
        }
    }

    /**
     * creates a template that only participates in outside transactions, no implicit transactions are started
     * @param graphDatabaseService the neo4j graph database
     * @return a Neo4jTemplate instance
     */
    public static Neo4jTemplate templateWithExplicitTransactions(GraphDatabaseService graphDatabaseService) {
        return new Neo4jTemplate(graphDatabaseService,true);
    }

    /**
     * creates a template that creates implicit transactions for its methods, including exec. If an outside transaction
     * is running those participate in the outside transaction.
     * @param graphDatabaseService the neo4j graph database
     */
    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        this(graphDatabaseService,false);
    }

    /**
     * @param graphDatabaseService the neo4j graph database
     * @param useExplicitTransactions if set the template only participates in outside transactions,
     * no internal implicit transactions are started
     * @return a Neo4jTemplate instance
     */
    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService, boolean useExplicitTransactions) {
        notNull(graphDatabaseService, "graphDatabaseService");
        this.useExplicitTransactions = useExplicitTransactions;
        this.graphDatabaseService = graphDatabaseService;
        index = this.graphDatabaseService.index();
    }

    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return exceptionTranslator.translateExceptionIfPossible(ex);
    }

    private Transaction beginTx() {
        if (useExplicitTransactions) {
            return new NullTransaction();
        }
        return graphDatabaseService.beginTx();
    }


    @Override
    public <T> T exec(final GraphCallback<T> callback) {
        notNull(callback, "callback");
        Transaction tx = beginTx();
        try {
            T result = callback.doWithGraph(graphDatabaseService);
            tx.success();
            return result;
        } catch (RuntimeException e) {
            tx.failure();
            throw translateExceptionIfPossible(e);
        } catch (Exception e) {
            throw new UncategorizedGraphStoreException("Error executing callback",e);
        } finally {
            tx.finish();
        }
    }

    @Override
    public Node getReferenceNode() {
        try {
            return graphDatabaseService.getReferenceNode();
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Node createNode(final Map<String, Object> properties, final String... indexFields) {
        return exec(new GraphCallback<Node>() {
            @Override
            public Node doWithGraph(GraphDatabaseService graph) throws Exception {
                Node node = graphDatabaseService.createNode();
                if (properties == null) return node;
                return autoIndex(null, setProperties(node, properties), indexFields);
            }
        });
    }

    @Override
    public Node getNode(long id) {
        if (id < 0) throw new InvalidDataAccessApiUsageException("id is negative");
        try {
            return graphDatabaseService.getNodeById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Relationship getRelationship(long id) {
        if (id < 0) throw new InvalidDataAccessApiUsageException("id is negative");
        try {
            return graphDatabaseService.getRelationshipById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T extends PropertyContainer> T autoIndex(String indexName, T element, String... indexFields) {
        for (String indexField : indexFields) {
            if (!element.hasProperty(indexField)) continue;
            index(indexName,element, indexField,element.getProperty(indexField));
        }
        return element;
    }

    @Override
    public <T extends PropertyContainer> T index(final String indexName, final T element, final String field, final Object value) {
        notNull(element, "element", field, "field", value, "value");
        exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                if (element instanceof Relationship) {
                    RelationshipIndex relationshipIndex = relationshipWriteIndex(indexName);
                    relationshipIndex.add((Relationship) element, field, value);
                } else if (element instanceof Node) {
                    nodeIndex(indexName).add((Node) element, field, value);
                } else {
                    throw new IllegalArgumentException("Provided element is neither node nor relationship " + element);
                }
            }
        });
        return element;
    }

    private RelationshipIndex relationshipWriteIndex(String indexName) {
        if (indexName == null) {
            return index.forRelationships("relationship");
        }
        return index.forRelationships(indexName);
    }

    private RelationshipIndex relationshipIndex(String indexName) {
        if (indexName != null && index.existsForRelationships(indexName)) {
            return index.forRelationships(indexName);
        }
        return null;
    }

    @Override
    public <T> Iterable<T> query(String indexName, final PathMapper<T> pathMapper, Object queryOrQueryObject) {
        notNull(queryOrQueryObject, "queryOrQueryObject", pathMapper, "pathMapper");
        try {
            RelationshipIndex relationshipIndex = relationshipIndex(indexName);
            if (relationshipIndex!=null) {
                return mapRelationships(relationshipIndex.query(queryOrQueryObject), pathMapper);
            }
            return mapNodes(nodeIndex(indexName).query(queryOrQueryObject), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> query(String indexName, final PathMapper<T> pathMapper, String field, String value) {
        notNull(field, "field", value, "value", pathMapper, "pathMapper");
        try {
            RelationshipIndex relationshipIndex = relationshipIndex(indexName);
            if (relationshipIndex!=null) {
                return mapRelationships(relationshipIndex.get(field, value), pathMapper);
            }
            return mapNodes(nodeIndex(indexName).get(field, value), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapNodes(final Iterable<Node> nodes, final PathMapper<T> pathMapper) {
        assert nodes != null;
        assert pathMapper != null;
        return new IterableWrapper<T, Node>(nodes) {
            @Override
            protected T underlyingObjectToObject(Node node) {
                return pathMapper.mapPath(new NodePath(node));
            }
        };
    }

    private Index<Node> nodeIndex(String indexName) {
        return index.forNodes(indexName == null ? "node" : indexName);
    }

    @Override
    public <T> Iterable<T> traverseGraph(Node startNode, final PathMapper<T> pathMapper, TraversalDescription traversal) {
        notNull(startNode, "startNode", traversal, "traversal", pathMapper, "pathMapper");
        try {
            return mapPaths(traversal.traverse(startNode), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapPaths(final Iterable<Path> paths, final PathMapper<T> pathMapper) {
        return new PathMappingIterator().mapPaths(paths,pathMapper);
    }


    @Override
    public <T> Iterable<T> traverseNext(Node startNode, final PathMapper<T> pathMapper, RelationshipType relationshipType, Direction direction) {
        notNull(startNode, "startNode", relationshipType, "relationshipType", direction, "direction", pathMapper, "pathMapper");
        try {
            return mapRelationships(startNode.getRelationships(relationshipType, direction), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> traverseNext(Node startNode, final PathMapper<T> pathMapper, RelationshipType... relationshipTypes) {
        notNull(startNode, "startNode", relationshipTypes, "relationshipType", pathMapper, "pathMapper");
        try {
            return mapRelationships(startNode.getRelationships(relationshipTypes), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> traverseNext(Node startNode, final PathMapper<T> pathMapper) {
        notNull(startNode, "startNode", pathMapper, "pathMapper");
        try {
            return mapRelationships(startNode.getRelationships(), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapRelationships(final Iterable<Relationship> relationships, final PathMapper<T> pathMapper) {
        assert relationships != null;
        assert pathMapper != null;
        return new IterableWrapper<T, Relationship>(relationships) {
            @Override
            protected T underlyingObjectToObject(Relationship relationship) {
                return pathMapper.mapPath(new RelationshipPath(relationship));
            }
        };
    }

    @Override
    public Relationship createRelationship(final Node startNode, final Node endNode, final RelationshipType relationshipType, final Map<String, Object> properties, final String... indexFields) {
        notNull(startNode, "startNode", endNode, "endNode", relationshipType, "relationshipType", properties, "properties");
        return exec(new GraphCallback<Relationship>() {
            @Override
            public Relationship doWithGraph(GraphDatabaseService graph) throws Exception {
                Relationship relationship = startNode.createRelationshipTo(endNode, relationshipType);
                if (properties == null) return relationship;
                return autoIndex("relationship", setProperties(relationship, properties), indexFields);
            }
        });
    }

    private <T extends PropertyContainer> T setProperties(T primitive, Map<String, Object> properties) {
        assert primitive != null;
        if (properties==null) return primitive;
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if (prop.getValue()==null) {
                primitive.removeProperty(prop.getKey());
            } else {
                primitive.setProperty(prop.getKey(), prop.getValue());
            }
        }
        return primitive;
    }

    private static class NullTransaction implements Transaction {
        @Override
        public void failure() {

        }

        @Override
        public void success() {

        }

        @Override
        public void finish() {

        }
    }
}
