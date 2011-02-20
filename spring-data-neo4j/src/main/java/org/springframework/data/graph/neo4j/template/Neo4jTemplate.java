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
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

import java.util.Arrays;

public class Neo4jTemplate implements Neo4jOperations, PersistenceExceptionTranslator {

    private final GraphDatabaseService graphDatabaseService;

    private final Neo4jExceptionTranslator exceptionTranslator = new Neo4jExceptionTranslator();

    private static void notNull(Object... pairs) {
        assert pairs.length % 2 == 0 : "wrong number of pairs to check";
        for (int i = 0; i < pairs.length; i += 2) {
            if (pairs[i] == null) {
                throw new InvalidDataAccessApiUsageException("[Assertion failed] - " + pairs[i + 1] + " is required; it must not be null");
            }
        }
    }

    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        notNull(graphDatabaseService, "graphDatabaseService");
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return exceptionTranslator.translateExceptionIfPossible(ex);
    }

    @Override
    public <T> T doInTransaction(final GraphTransactionCallback<T> callback) {
        notNull(callback, "callback");
        return execute(callback);
    }


    @Override
    public <T> T execute(final GraphCallback<T> callback) {
        notNull(callback, "callback");
        try {
            return callback.doWithGraph(graphDatabaseService);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
    public Node createNode(final Property... properties) {
        notNull(properties, "properties");
        return doInTransaction(new GraphTransactionCallback<Node>() {
            @Override
            public Node doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                return setProperties(graphDatabaseService.createNode(), properties);
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
    public void index(final Relationship relationship, final String indexName, final String field, final Object value) {
        notNull(relationship, "relationship", field, "field", value, "value");
        doInTransaction(new GraphTransactionCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception {
                relationshipIndex(indexName).add(relationship, field, value);
            }
        });
    }

    @Override
    public void index(final Node node, final String indexName, final String field, final Object value) {
        notNull(node, "node", field, "field", value, "value");
        doInTransaction(new GraphTransactionCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception {
                nodeIndex(indexName).add(node, field, value);
            }
        });
    }

    private RelationshipIndex relationshipIndex(String indexName) {
        return graphDatabaseService.index().forRelationships(indexName == null ? "relationship" : indexName);
    }

    @Override
    public <T> Iterable<T> queryNodes(String indexName, Object queryOrQueryObject, final PathMapper<T> pathMapper) {
        notNull(queryOrQueryObject, "queryOrQueryObject", pathMapper, "pathMapper");
        try {
            return mapNodes(nodeIndex(indexName).query(queryOrQueryObject), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> retrieveNodes(String indexName, String field, String value, final PathMapper<T> pathMapper) {
        notNull(field, "field", value, "value", pathMapper, "pathMapper");
        try {
            return mapNodes(nodeIndex(indexName).get(field, value), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> queryRelationships(String indexName, Object queryOrQueryObject, final PathMapper<T> pathMapper) {
        notNull(queryOrQueryObject, "queryOrQueryObject", pathMapper, "pathMapper");
        try {
            return mapRelationships(relationshipIndex(indexName).query(queryOrQueryObject), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> retrieveRelationships(String indexName, String field, String value, final PathMapper<T> pathMapper) {
        notNull(field, "field", value, "value", pathMapper, "pathMapper");
        try {
            return mapRelationships(relationshipIndex(indexName).get(field, value), pathMapper);
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
        return graphDatabaseService.index().forNodes(indexName == null ? "node" : indexName);
    }

    @Override
    public <T> Iterable<T> traverse(Node startNode, TraversalDescription traversal, final PathMapper<T> pathMapper) {
        notNull(startNode, "startNode", traversal, "traversal", pathMapper, "pathMapper");
        try {
            return mapPaths(traversal.traverse(startNode), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapPaths(final Iterable<Path> paths, final PathMapper<T> pathMapper) {
        assert paths != null;
        assert pathMapper != null;
        return new IterableWrapper<T, Path>(paths) {
            @Override
            protected T underlyingObjectToObject(Path path) {
                return pathMapper.mapPath(path);
            }
        };
    }

    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, RelationshipType relationshipType, Direction direction, final PathMapper<T> pathMapper) {
        notNull(startNode, "startNode", relationshipType, "relationshipType", direction, "direction", pathMapper, "pathMapper");
        try {
            return mapRelationships(startNode.getRelationships(relationshipType, direction), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, final PathMapper<T> pathMapper, RelationshipType... relationshipTypes) {
        notNull(startNode, "startNode", relationshipTypes, "relationshipType", pathMapper, "pathMapper");
        try {
            return mapRelationships(startNode.getRelationships(relationshipTypes), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, final PathMapper<T> pathMapper) {
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
    public Relationship createRelationship(final Node startNode, final Node endNode, final RelationshipType relationshipType, final Property... properties) {
        notNull(startNode, "startNode", endNode, "endNode", relationshipType, "relationshipType", properties, "properties");
        return doInTransaction(new GraphTransactionCallback<Relationship>() {
            @Override
            public Relationship doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                return setProperties(startNode.createRelationshipTo(endNode, relationshipType), properties);
            }
        });
    }

    private <T extends PropertyContainer> T setProperties(T primitive, Property... properties) {
        assert primitive != null;
        assert properties != null;
        for (Property prop : properties) {
            if (prop == null)
                throw new IllegalArgumentException("at least one Property is null: " + Arrays.toString(properties));
            primitive.setProperty(prop.getName(), prop.getValue());
        }
        return primitive;
    }
}
