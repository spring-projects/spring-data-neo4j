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
import org.neo4j.graphdb.traversal.*;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.graph.UncategorizedGraphStoreException;

import java.util.Arrays;

public class Neo4jTemplate implements Neo4jOperations {

    private final GraphDatabaseService graphDatabaseService;

    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        if (graphDatabaseService == null)
            throw new IllegalArgumentException("GraphDatabaseService must not be null");
        this.graphDatabaseService = graphDatabaseService;
    }

    public DataAccessException translateExceptionIfPossible(Exception ex) {
        // todo delete, duplicate semantics
        try {
            throw ex;
        } catch(IllegalArgumentException iae) {
            throw iae;
        } catch(DataAccessException dae) {
            throw dae;
        } catch(NotInTransactionException nit) {
            throw new UncategorizedGraphStoreException("Not in transaction", nit);
        } catch(TransactionFailureException tfe) {
            throw new UncategorizedGraphStoreException("Transaction Failure", tfe);
        } catch(NotFoundException nfe) {
            throw new DataRetrievalFailureException("Not Found", nfe);
        } catch(Exception e) {
            throw new UncategorizedGraphStoreException("Error executing graph operation", ex);
        }
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
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Node getReferenceNode() {
        try {
            return graphDatabaseService.getReferenceNode();
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public Relationship getRelationship(long id) {
        try {
            return graphDatabaseService.getRelationshipById(id);
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }

    @Override
    public <T> Iterable<T> queryNodes(String indexName, Object queryOrQueryObject, final PathMapper<T> pathMapper) {
        try {
            return mapNodes(nodeIndex(indexName).query(queryOrQueryObject), pathMapper);
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> retrieveNodes(String indexName, String field, String value, final PathMapper<T> pathMapper) {
        try {
            return mapNodes(nodeIndex(indexName).get(field, value), pathMapper);
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> queryRelationships(String indexName, Object queryOrQueryObject, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(relationshipIndex(indexName).query(queryOrQueryObject),pathMapper);
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> retrieveRelationships(String indexName, String field, String value, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(relationshipIndex(indexName).get(field, value),pathMapper);
        } catch (Exception e) {
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, final PathMapper<T> pathMapper, RelationshipType... type) {
        try {
            return mapRelationships(startNode.getRelationships(type), pathMapper);
        } catch (Exception e) {
            throw translateExceptionIfPossible(e);
        }
    }
    @Override
    public <T> Iterable<T> traverseDirectRelationships(Node startNode, final PathMapper<T> pathMapper) {
        try {
            return mapRelationships(startNode.getRelationships(), pathMapper);
        } catch (Exception e) {
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
