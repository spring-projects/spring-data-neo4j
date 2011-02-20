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
import org.springframework.dao.support.PersistenceExceptionTranslator;

import java.util.Arrays;

public class Neo4jTemplate implements Neo4jOperations, PersistenceExceptionTranslator {

    private final GraphDatabaseService graphDatabaseService;

    private final Neo4jExceptionTranslator exceptionTranslator = new Neo4jExceptionTranslator();
    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        if (graphDatabaseService == null)
            throw new IllegalArgumentException("GraphDatabaseService must not be null");
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return exceptionTranslator.translateExceptionIfPossible(ex);
    }

    @Override
    public <T> T doInTransaction(final GraphTransactionCallback<T> callback) {
        if (callback == null)
            throw new IllegalArgumentException("Callback must not be null");
        return execute(callback);
    }


    @Override
    public <T> T execute(final GraphCallback<T> callback) {
        if (callback == null)
            throw new IllegalArgumentException("Callback must not be null");
        try {
            return callback.doWithGraph(graphDatabaseService);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        } catch(Exception e) {
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
    public Node createNode(final Property... props) {
        return doInTransaction(new GraphTransactionCallback<Node>(){
            @Override
            public Node doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
               return setProperties(graphDatabaseService.createNode(),props);
            }
        });
    }

    @Override
    public Node getNode(long id) {
        try {
            return graphDatabaseService.getNodeById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Relationship getRelationship(long id) {
        try {
            return graphDatabaseService.getRelationshipById(id);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public void index(final PropertyContainer primitive, final String indexName, final String field, final Object value) {
        doInTransaction(new GraphTransactionCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(Status status, GraphDatabaseService graph) throws Exception {
                if (primitive instanceof Node) {
                    nodeIndex(indexName).add((Node) primitive, field, value);
                    return;
                }
                if (primitive instanceof Relationship) {
                    relationshipIndex(indexName).add((Relationship) primitive, field, value);
                    return;
                }
                throw new IllegalArgumentException("Supplied graph primitive is invalid");
            }
        });
    }

    private RelationshipIndex relationshipIndex(String indexName) {
        return graphDatabaseService.index().forRelationships(indexName == null ? "relationship" : indexName);
    }

    @Override
    public void index(PropertyContainer primitive, String field, Object value) {
        try {
            index(primitive,null,field,value);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> queryNodes(String indexName, Object queryOrQueryObject, final PathMapper<T> pathMapper) {
        try {
            return mapNodes(nodeIndex(indexName).query(queryOrQueryObject), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> retrieveNodes(String indexName, String field, String value, final PathMapper<T> pathMapper) {
        try {
            return mapNodes(nodeIndex(indexName).get(field, value), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> queryRelationships(String indexName, Object queryOrQueryObject, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(relationshipIndex(indexName).query(queryOrQueryObject), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> retrieveRelationships(String indexName, String field, String value, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(relationshipIndex(indexName).get(field, value), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapNodes(final Iterable<Node> nodes, final PathMapper<T> pathMapper) {
        return new IterableWrapper<T,Node>(nodes) {
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
        try {
            return mapPaths(traversal.traverse(startNode), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapPaths(final Iterable<Path> paths, final PathMapper<T> pathMapper) {
        return new IterableWrapper<T, Path>(paths) {
            @Override
            protected T underlyingObjectToObject(Path path) {
                return pathMapper.mapPath(path);
            }
        };
    }

    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, RelationshipType type, Direction direction, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(startNode.getRelationships(type, direction), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, final PathMapper<T> pathMapper, RelationshipType... type) {
        try {
            return mapRelationships(startNode.getRelationships(type), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(startNode.getRelationships(), pathMapper);
        } catch (RuntimeException e) {
            throw translateExceptionIfPossible(e);
        }
    }

    private <T> Iterable<T> mapRelationships(final Iterable<Relationship> relationships, final PathMapper<T> pathMapper) {
        return new IterableWrapper<T, Relationship>(relationships) {
            @Override
            protected T underlyingObjectToObject(Relationship relationship) {
                return pathMapper.mapPath(new RelationshipPath(relationship));
            }
        };
    }

    @Override
    public Relationship createRelationship(final Node startNode, final Node endNode, final RelationshipType type, final Property... props) {
        return doInTransaction(new GraphTransactionCallback<Relationship>(){
            @Override
            public Relationship doWithGraph(Status status, GraphDatabaseService graph) throws Exception {
                return setProperties(startNode.createRelationshipTo(endNode, type), props);
            }
        });
    }

    private <T extends PropertyContainer> T setProperties(T primitive, Property... props) {
        if (props==null) throw new IllegalArgumentException("props array is null");
        for (Property prop : props) {
            if (prop==null) throw new IllegalArgumentException("at least one Property is null: "+ Arrays.toString(props));
            primitive.setProperty(prop.getName(), prop.getValue());
        }
        return primitive;
    }
}
