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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.data.graph.neo4j.template.IterationController.IterationControl.EAGER_STOP_ON_NULL;

public class Neo4jTemplate implements Neo4jOperations {

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

    public Neo4jTemplate(final GraphDatabaseService graphDatabaseService) {
        notNull(graphDatabaseService, "graphDatabaseService");
        this.graphDatabaseService = graphDatabaseService;
        index = this.graphDatabaseService.index();
    }

    public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return exceptionTranslator.translateExceptionIfPossible(ex);
    }

    @Override
    public <T> T update(final GraphCallback<T> callback) {
        notNull(callback, "callback");
        Transaction tx = graphDatabaseService.beginTx();
        try {
            T result = exec(callback);
            tx.success();
            return result;
        } catch (RuntimeException e) {
            tx.failure();
            throw e;
        } finally {
            tx.finish();
        }
    }


    @Override
    public <T> T exec(final GraphCallback<T> callback) {
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
    public Node createNode(final Map<String, Object> properties, final String... indexFields) {
        return update(new GraphCallback<Node>() {
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
        update(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                RelationshipIndex relationshipIndex = relationshipIndex(indexName);
                if (relationshipIndex != null && element instanceof Relationship) {
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
        assert paths != null;
        assert pathMapper != null;
        IterationController.IterationControl control = getIterationControl(pathMapper);
        switch (control) {
            case EAGER:
            case EAGER_STOP_ON_NULL:
                List<T> result=new ArrayList<T>();
                for (Path path : paths) {
                    T mapped = pathMapper.mapPath(path);
                    if (mapped==null && control== EAGER_STOP_ON_NULL) break;
                    result.add(mapped);
                }
                return result;
            case LAZY:
                return new IterableWrapper<T, Path>(paths) {
                    @Override
                    protected T underlyingObjectToObject(Path path) {
                        return pathMapper.mapPath(path);
                    }
                };
            default: throw new IllegalStateException("Unknown IterationControl "+control);
        }
    }

    private <T> IterationController.IterationControl getIterationControl(PathMapper<T> pathMapper) {
        if (pathMapper instanceof IterationController) {
            IterationController.IterationControl result = ((IterationController) pathMapper).iterateAs();
            if (result!=null) return result;
        }
        return IterationController.IterationControl.LAZY;
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
        return update(new GraphCallback<Relationship>() {
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
}
