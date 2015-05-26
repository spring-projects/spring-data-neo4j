/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;

import org.apache.lucene.search.Query;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.index.impl.lucene.AbstractIndexHits;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.IndexInfo;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.query.*;
import org.neo4j.rest.graphdb.transaction.TransactionFinishListener;
import org.neo4j.rest.graphdb.traversal.RestTraversalDescription;
import org.neo4j.rest.graphdb.traversal.RestTraverser;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.neo4j.rest.graphdb.util.QueryResultBuilder;
import org.neo4j.rest.graphdb.util.ResultConverter;

import java.util.*;

import static java.util.Arrays.asList;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.rest.graphdb.query.CypherTransaction.ResultType.row;
import static org.neo4j.rest.graphdb.query.CypherTransaction.Statement;


public class RestAPICypherImpl implements RestAPI {

    public static final String _QUERY_RETURN_NODE = " RETURN id(n) as id, labels(n) as labels, n as properties";
    public static final String _QUERY_RETURN_REL = " RETURN id(r) as id, type(r) as type, r as properties, id(startNode(r)) as start, id(endNode(r)) as end";

    public static String MATCH_NODE_QUERY(String name) {
        return " MATCH (" + name + ") WHERE id(" + name + ") = {id_" + name + "} ";
    }

    public static final String _MATCH_NODE_QUERY = " MATCH (n) WHERE id(n) = {id} ";
    public static final String GET_NODE_QUERY = _MATCH_NODE_QUERY + _QUERY_RETURN_NODE;
    public static final String _MATCH_REL_QUERY = " START r=rel({id}) ";
    public static final String GET_REL_QUERY = _MATCH_REL_QUERY + _QUERY_RETURN_REL;

    public static final String GET_REL_TYPES_QUERY = _MATCH_NODE_QUERY + " MATCH (n)-[r]-() RETURN distinct type(r) as relType";

    private RestIndexManager restIndex = new RestIndexManager(this);
    private RestIndexManager restIndexOld;

    private String createNodeQuery(Collection<String> labels) {
        String labelString = toLabelString(labels);
        return "CREATE (n" + labelString + " {props}) " + _QUERY_RETURN_NODE;
    }

    private String mergeQuery(String labelName, String key, Collection<String> labels) {
        StringBuilder setLabels = new StringBuilder();
        if (labels != null) {
            for (String label : labels) {
                if (label.equals(labelName)) continue;
                setLabels.append("SET n:").append(label).append(" ");
            }
        }
        return "MERGE (n:`" + labelName + "` {`" + key + "`: {value}}) ON CREATE SET n={props} " + setLabels + _QUERY_RETURN_NODE;
    }

    private String toLabelString(Collection<String> labels) {
        if (labels == null || labels.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String label : labels) {
            sb.append(":").append(label);
        }
        return sb.toString();
    }

    private final RestAPI restAPI;

    private final RestCypherTransactionManager txManager = new RestCypherTransactionManager(this);

    protected RestAPICypherImpl(RestAPI restAPI) {
        this.restAPI = restAPI;
        restIndexOld = new RestIndexManager(restAPI);
    }

    @Override
    public RestNode getNodeById(long id, Load force) {
        if (force != Load.ForceFromServer) {
            RestNode restNode = getNodeFromCache(id);
            if (restNode != null) return restNode;
        }
        if (force == Load.FromCache) return new RestNode(RestNode.nodeUri(this, id), this);
        Iterator<List<Object>> result = runQuery(GET_NODE_QUERY, map("id", id)).getRows().iterator();
        if (!result.hasNext()) {
            throw new NotFoundException("Node not found " + id);
        }
        List<Object> row = result.next();
        return addToCache(toNode(row));
    }

    @Override
    public RestRelationship addToCache(RestRelationship restRelationship) {
        return restAPI.addToCache(restRelationship);
    }

    @Override
    public RestRelationship getRelationshipById(long id, Load force) {
        if (force != Load.ForceFromServer) {
            RestRelationship restRel = getRelFromCache(id);
            if (restRel != null) return restRel;
        }
        if (force == Load.FromCache) return new RestRelationship(RestRelationship.relUri(this, id), this);
        try {
            Iterator<List<Object>> result = runQuery(GET_REL_QUERY, map("id", id)).getRows().iterator();
            if (!result.hasNext()) {
                throw new NotFoundException("Relationship not found " + id);
            }
            List<Object> row = result.next();
            return addToCache(toRel(row));
        } catch (NotFoundException e) {
            throw e;
        } catch (CypherTransactionExecutionException ctee) {
            if (ctee.contains("Neo.DatabaseError.Statement.ExecutionFailure","not found")) {
                throw new NotFoundException("Relationship not found " + id);
            }
            throw ctee;
        }
    }

    public RestNode getNodeFromCache(long id) {
        return restAPI.getNodeFromCache(id);
    }
    public RestRelationship getRelFromCache(long id) {
        return restAPI.getRelFromCache(id);
    }

    @Override
    public void removeNodeFromCache(long id) {
        restAPI.removeNodeFromCache(id);
    }
    @Override
    public void removeRelFromCache(long id) {
        restAPI.removeRelFromCache(id);
    }

    @Override
    public RestNode getNodeById(long id) {
        return getNodeById(id, Load.FromServer);
    }

    @Override
    public RestRelationship getRelationshipById(long id) {
        return getRelationshipById(id, Load.FromServer);
    }


    private RestNode toNode(List<Object> row) {
        long id = ((Number) row.get(0)).longValue();
        List<String> labels = (List<String>) row.get(1);
        Map<String, Object> props = (Map<String, Object>) row.get(2);
        return RestNode.fromCypher(id, labels, props, this);
    }

    private RestRelationship toRel(List<Object> row) {
        long id = ((Number) row.get(0)).longValue();
        String type = (String) row.get(1);
        Map<String, Object> props = (Map<String, Object>) row.get(2);
        long start = ((Number) row.get(3)).longValue();
        long end = ((Number) row.get(4)).longValue();
        return RestRelationship.fromCypher(id, type, props, start, end, this);
    }

    @Override
    public RestNode createNode(Map<String, Object> props) {
        return createNode(props, Collections.<String>emptyList());
    }

    @Override
    public RestNode createNode(Map<String, Object> props, Collection<String> labels) {
        Iterator<List<Object>> result = runQuery(createNodeQuery(labels), map("props", props(props))).getRows().iterator();
        if (result.hasNext()) {
            return addToCache(toNode(result.next()));
        }
        throw new RuntimeException("Error creating node with labels: " + labels + " and props: " + props + " no data returned");
    }

    @Override
    public RestNode merge(String labelName, String key, Object value, Map<String, Object> nodeProperties, Collection<String> labels) {
        if (labelName == null || key == null || value == null)
            throw new IllegalArgumentException("Label " + labelName + " key " + key + " and value must not be null");
        nodeProperties = props(nodeProperties);
        Map props = nodeProperties.containsKey(key) ? nodeProperties : MapUtil.copyAndPut(nodeProperties, key, value);
        Map<String, Object> params = map("props", props, "value", value);
        Iterator<List<Object>> result = runQuery(mergeQuery(labelName, key, labels), params).getRows().iterator();
        if (!result.hasNext())
            throw new RuntimeException("Error merging node with labels: " + labelName + " key " + key + " value " + value + " labels " + labels + " and props: " + props + " no data returned");

        return addToCache(toNode(result.next()));
    }

    public RestNode addToCache(RestNode restNode) {
        return restAPI.addToCache(restNode);
    }

    @Override
    public RestRelationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props) {
        String statement = MATCH_NODE_QUERY("n") + MATCH_NODE_QUERY("m") + " CREATE (n)-[r:`" + type.name() + "`]->(m) SET r={props} " + _QUERY_RETURN_REL;
        Map<String, Object> params = map("id_n", startNode.getId(), "id_m", endNode.getId(), "props", props(props));
        CypherTransaction.Result result = runQuery(statement, params);
        if (!result.hasData())
            throw new RuntimeException("Error creating relationship from " + startNode + " to " + endNode + " type " + type.name());
        Iterator<List<Object>> it = result.getRows().iterator();
        return toRel(it.next());
    }


    @Override
    public void removeLabel(RestNode node, String label) {
        CypherTransaction.Result result = runQuery(_MATCH_NODE_QUERY + (" REMOVE n:`" + label + "` ") + _QUERY_RETURN_NODE, map("id", node.getId()));
        if (!result.hasData()) {
            throw new RuntimeException("Error removing label " + label + " from node " + node);
        }
    }

    @Override
    public Iterable<RestNode> getNodesByLabel(String label) {
        String statement = "MATCH (n:`" + label + "`) " + _QUERY_RETURN_NODE;
        return queryForNodes(statement, null);
    }

    private Iterable<RestNode> queryForNodes(String statement, Map<String, Object> params) {
        Iterable<List<Object>> result = runQuery(statement, params).getRows();
        return new IterableWrapper<RestNode, List<Object>>(result) {
            protected RestNode underlyingObjectToObject(List<Object> row) {
                return addToCache(toNode(row));
            }
        };
    }

    @Override
    public Iterable<RestNode> getNodesByLabelAndProperty(String label, String property, Object value) {
        String statement = "MATCH (n:`" + label + "`) WHERE n.`" + property + "` = {value} " + _QUERY_RETURN_NODE;
        return queryForNodes(statement, map("value", value));
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes(RestNode node) {
        Iterable<List<Object>> result = runQuery(GET_REL_TYPES_QUERY, map("id", node.getId())).getRows();
        return new IterableWrapper<RelationshipType, List<Object>>(result) {
            protected RelationshipType underlyingObjectToObject(List<Object> row) {
                return DynamicRelationshipType.withName(row.get(0).toString());
            }
        };
    }

    @Override
    public int getDegree(RestNode restNode, RelationshipType type, Direction direction) {
        String nodeDegreeQuery = "MATCH (n)" + relPattern(direction, type) + "() WHERE id(n) = {id} RETURN count(*) as degree";
        Iterator<List<Object>> degree = runQuery(nodeDegreeQuery, map("id", restNode.getId())).getRows().iterator();
        if (!degree.hasNext()) return 0;
        return ((Number) degree.next().get(0)).intValue();
    }

    private String relPattern(Direction direction, RelationshipType... types) {
        String typeString = toTypeString(types);
        String relPattern = "-[r]-";
        if (!typeString.isEmpty()) relPattern = "-[r " + typeString + "]-";
        if (direction == Direction.OUTGOING) {
            relPattern += ">";
        } else if (direction == Direction.INCOMING) {
            relPattern = "<" + relPattern;
        }
        return relPattern;
    }

    private String toTypeString(RelationshipType... types) {
        if (types == null || types.length == 0) return "";
        StringBuilder typeString = new StringBuilder();
        for (RelationshipType type : types) {
            if (type==null) continue;
            if (typeString.length() > 0) typeString.append("|");
            typeString.append(':').append('`').append(type.name()).append("`");
        }
        return typeString.toString();
    }

    @Override
    public Iterable<Relationship> getRelationships(RestNode restNode, Direction direction, RelationshipType... types) {
        String statement = _MATCH_NODE_QUERY + " MATCH (n)" + relPattern(direction, types) + "() " + _QUERY_RETURN_REL;
        CypherTransaction.Result result = runQuery(statement, map("id", restNode.getId()));
        return new IterableWrapper<Relationship, List<Object>>(result.getRows()) {
            protected Relationship underlyingObjectToObject(List<Object> row) {
                return toRel(row);
            }
        };
    }

    @Override
    public void addLabels(RestNode node, Collection<String> labels) {
        String statement = _MATCH_NODE_QUERY + " SET n" + toLabelString(labels) + _QUERY_RETURN_NODE;
        CypherTransaction.Result result = runQuery(statement, map("id", node.getId()));

        if (!result.hasData()) {
            throw new RuntimeException("Error adding labels " + labels + " to node " + node);
        }
    }

    public RestRequest getRestRequest() {
        return restAPI.getRestRequest();
    }

    @Override
    public Transaction beginTx() {
        return txManager.beginTx();
    }

    public RestCypherTransactionManager getTxManager() {
        return txManager;
    }

    @Override
    public <S extends PropertyContainer> IndexHits<S> getIndex(Class<S> entityType, String indexName, String key, Object value) {
        if (value instanceof Query) return restAPI.getIndex(entityType, indexName, key, value);
        String index = key == null ? ":`" + indexName + "`({query})" : ":`" + indexName + "`(`" + key + "`={query})";
        if (Node.class.isAssignableFrom(entityType)) {
            String statement = "start n=node" + index + _QUERY_RETURN_NODE;
            CypherTransaction.Result result = runQuery(statement, map("query", value));
            return toIndexHits(result, true);
        }
        if (Relationship.class.isAssignableFrom(entityType)) {
            String statement = "start r=rel" + index + _QUERY_RETURN_REL;
            CypherTransaction.Result result = runQuery(statement, map("query", value));
            return toIndexHits(result, false);
        }
        throw new IllegalStateException("Unknown index entity type " + entityType);
    }

    @Override
    public <S extends PropertyContainer> IndexHits<S> queryIndex(Class<S> entityType, String indexName, String key, Object value) {
        if (value instanceof Query) return restAPI.queryIndex(entityType,indexName,key,value);
        String index = ":`" + indexName + "`({query})";
        if (key != null && !key.isEmpty() && !value.toString().contains(":")) value = key + ":"+value;
        if (Node.class.isAssignableFrom(entityType)) {
            String statement = "start n=node" + index + _QUERY_RETURN_NODE;
            CypherTransaction.Result result = runQuery(statement, map("query", value));
            return toIndexHits(result, true);
        }
        if (Relationship.class.isAssignableFrom(entityType)) {
            String statement = "start r=rel" + index + _QUERY_RETURN_REL;
            CypherTransaction.Result result = runQuery(statement, map("query", value));
            return toIndexHits(result, false);
        }
        throw new IllegalStateException("Unknown index entity type " + entityType);
    }

    private <S extends PropertyContainer> IndexHits<S> toIndexHits(CypherTransaction.Result result, final boolean isNode) {
        final int size = IteratorUtil.count(result.getRows());
        final Iterator<List<Object>> it = result.getRows().iterator();
        return new AbstractIndexHits<S>() {
            @Override
            public int size() {
                return size;
            }

            @Override
            public float currentScore() {
                return 0;
            }

            @Override
            protected S fetchNextOrNull() {
                if (!it.hasNext()) return null;
                return (S) (isNode ? addToCache(toNode(it.next())) : toRel(it.next()));
            }
        };
    }

    @Override
    public RestIndexManager index() {
        return restIndex;
    }


    @Override
    public void deleteEntity(RestEntity entity) {
        if (entity instanceof Node) {
            runQuery(_MATCH_NODE_QUERY + " DELETE n", map("id", entity.getId()));
            restAPI.removeNodeFromCache(entity.getId());
        } else if (entity instanceof Relationship) {
            runQuery(_MATCH_REL_QUERY + " DELETE r", map("id", entity.getId()));
            restAPI.removeRelFromCache(entity.getId());
        }
    }

    @Override
    public void setPropertyOnEntity(RestEntity entity, String key, Object value) {
        if (entity instanceof Node) {
            runQuery(_MATCH_NODE_QUERY + " SET n.`" + key + "` = {value} ", map("id", entity.getId(), "value", value));
        } else if (entity instanceof Relationship) {
            runQuery(_MATCH_REL_QUERY + " SET r.`" + key + "` = {value} ", map("id", entity.getId(), "value", value));
        }
    }

    // TODO return entity ???
    @Override
    public void setPropertiesOnEntity(RestEntity entity, Map<String, Object> properties) {
        if (entity instanceof Node) {
            runQuery(_MATCH_NODE_QUERY + " SET n = {props} ", map("id", entity.getId(), "props", properties));
        } else if (entity instanceof Relationship) {
            runQuery(_MATCH_REL_QUERY + " SET r = {props} ", map("id", entity.getId(), "props", properties));
        }
    }

    @Override
    public void removeProperty(RestEntity entity, String key) {
        if (entity instanceof Node) {
            runQuery(_MATCH_NODE_QUERY + " REMOVE n.`" + key + "`", map("id", entity.getId()));
        } else if (entity instanceof Relationship) {
            runQuery(_MATCH_REL_QUERY + " REMOVE r.`" + key + "`", map("id", entity.getId()));
        }
    }

    // todo handle within cypher tx
    @Override
    public RestNode getOrCreateNode(RestIndex<Node> index, String key, Object value, final Map<String, Object> properties, Collection<String> labels) {
        return restAPI.getOrCreateNode(index, key, value, properties, labels);
    }

    // todo handle within cypher tx
    @Override
    public RestRelationship getOrCreateRelationship(RestIndex<Relationship> index, String key, Object value, final RestNode start, final RestNode end, final String type, final Map<String, Object> properties) {
        return restAPI.getOrCreateRelationship(index, key, value, start, end, type, properties);
    }

    public CypherResult query(String statement, Map<String, Object> params) {
        return new CypherTxResult(runQuery(statement, params,true));
    }

    private List<CypherTransaction.Result> runQueries(Collection<Statement> statements) {
        if (!txManager.isActive()) {
            CypherTransaction tx = newCypherTransaction();
            tx.addAll(statements);
            return tx.commit();
        } else {
            CypherTransaction tx = txManager.getCypherTransaction();
            tx.addAll(statements);
            return tx.send();
        }
    }

    private CypherTransaction.Result runQuery(String statement, Map<String, Object> params, boolean replace) {
        if (!txManager.isActive()) {
            return newCypherTransaction().commit(statement, params, replace);
        }
        return txManager.getCypherTransaction().send(statement, params, replace);
    }
    private CypherTransaction.Result runQuery(String statement, Map<String, Object> params) {
        return runQuery(statement,params,false);
    }

    public CypherTransaction newCypherTransaction() {
        return new CypherTransaction(this, row);
    }

    public QueryResult<Map<String, Object>> query(String statement, Map<String, Object> params, ResultConverter resultConverter) {
        CypherTransaction.Result result = runQuery(statement, params,true);
        Iterable it = new IterableWrapper<Map<String, Object>,Map<String, Object>>(result) {
            @Override
            protected Map<String, Object> underlyingObjectToObject(Map<String, Object> value) {
                return convertRestEntitiesInRow(value);
            }
        };
        return new QueryResultBuilder<>(it, resultConverter); // new RestEntityConverter(resultConverter));
    }

    private Map<String, Object> convertRestEntitiesInRow(Map<String, Object> value) {
        Map<String,Object> map= value;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object original = entry.getValue();
            if (!(original instanceof Map)) continue;
            Map mapValue = (Map) original;
            if (mapValue.containsKey("id") &&  mapValue.containsKey("properties")) {
                Object v = createRestEntity(mapValue);
                if (v != null) entry.setValue(v);
            }
        }
        return map;
    }

    class RestEntityConverter implements ResultConverter {
        ResultConverter delegate;

        public RestEntityConverter(ResultConverter delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object convert(Object value, Class type) {
            Map<String,Object> map= (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                Object v = doConvert(entry.getValue(), type);
                if (v != null) entry.setValue(v);
            }
            return map;
        }

        protected Object doConvert(Object value, Class type) {
            if (PropertyContainer.class.isAssignableFrom(type) && value instanceof Map) {
                return createRestEntity((Map)value);
            }
            return delegate.convert(value,type);
        }
    }

    @Override
    public RestTraverser traverse(RestNode restNode, Map<String, Object> description) {
        return restAPI.traverse(restNode, description);
    }

    public RequestResult batch(Collection<Map<String, Object>> batchRequestData) {
        return restAPI.batch(batchRequestData);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> RestIndex<T> getIndex(String indexName) {
        final RestIndexManager index = this.index();
        if (index.existsForNodes(indexName)) return (RestIndex<T>) index.forNodes(indexName);
        if (index.existsForRelationships(indexName)) return (RestIndex<T>) index.forRelationships(indexName);
        throw new IllegalArgumentException("Index " + indexName + " does not yet exist");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void createIndex(String type, String indexName, Map<String, String> config) {
        restAPI.createIndex(type, indexName, config);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> RestIndex<T> createIndex(Class<T> type, String indexName, Map<String, String> config) {
        resetIndex(type);
        if (Node.class.isAssignableFrom(type)) {
            return (RestIndex<T>) index().forNodes( indexName, config);
        }
        if (Relationship.class.isAssignableFrom(type)) {
            return (RestIndex<T>) index().forRelationships(indexName, config);
        }
        throw new IllegalArgumentException("Required Node or Relationship types to create index, got " + type);
    }

    @Override
    public void resetIndex(Class type) {
        restAPI.resetIndex(type);
    }

    @Override
    public void close() {
        restAPI.close();
    }

    @Override
    public Relationship getOrCreateRelationship(Node start, Node end, RelationshipType type, Direction direction, Map<String, Object> props) {
/*
        final Iterable<Relationship> existingRelationships = start.getRelationships(type, direction);
        for (final Relationship existingRelationship : existingRelationships) {
            if (existingRelationship != null && existingRelationship.getOtherNode(start).equals(end))
                return existingRelationship;
        }
        if (direction == Direction.INCOMING) {
            return end.createRelationshipTo(start, type);
        } else {
            return start.createRelationshipTo(end, type);
        }

 */
        String relPattern = relPattern(direction, type);
        String statement = MATCH_NODE_QUERY("n") + MATCH_NODE_QUERY("m") + " MERGE (n)"+relPattern+"(m) ON CREATE SET r={props}" + _QUERY_RETURN_REL;
        CypherTransaction.Result result = runQuery(statement, map("id_n", start.getId(), "id_m", end.getId(),"props", props(props)));
        if (!result.hasData())
            throw new RuntimeException("Error creating relationship from " + start + " to " + end + " type " + type.name() +" direction "+direction);
        return toRel(result.getRows().iterator().next());
    }

    @Override
    public Iterable<Relationship> updateRelationships(Node start, Collection<Node> endNodes, RelationshipType type, Direction direction, String targetLabel) {
        String targetLabelPredicate = targetLabel == null ? "" : " AND (m:`"+targetLabel+"` OR m:`_"+targetLabel+"`)";
        String relPattern = relPattern(direction, type);
        String statement1 = "MATCH (n)"+relPattern+"(m) WHERE id(n) = {id_n} "+targetLabelPredicate+" AND NOT id(m) IN {ids_m} DELETE r RETURN id(r) as id_r";
        String statement2 = MATCH_NODE_QUERY("n") + " MATCH (m) WHERE id(m) IN {ids_m} MERGE (n)"+relPattern+"(m)" + _QUERY_RETURN_REL;
        Map<String, Object> params = map("id_n", start.getId(), "ids_m", nodeIds(endNodes));
        List<CypherTransaction.Result> results = runQueries(asList(
                new Statement(statement1, params, row,false),
                new Statement(statement2, params, row,false)));
        Iterable<List<Object>> mergeResults = results.get(1).getRows();
        return new IterableWrapper<Relationship,List<Object>>(mergeResults) {
            @Override
            protected Relationship underlyingObjectToObject(List<Object> row) {
                return toRel(row);
            }
        };
    }

    private long[] nodeIds(Collection<Node> nodes) {
        long[] ids = new long[nodes.size()];
        int i=0;
        for (Node node : nodes) {
            ids[i++] = node.getId();
        }
        return ids;
    }

    public Map<String, Object> props(Map<String, Object> props) {
        return props == null ? Collections.<String, Object>emptyMap() : props;
    }

    @Override
    public boolean isAutoIndexingEnabled(Class<? extends PropertyContainer> clazz) {
        return restAPI.isAutoIndexingEnabled(clazz);
    }

    @Override
    public void setAutoIndexingEnabled(Class<? extends PropertyContainer> clazz, boolean enabled) {
        restAPI.setAutoIndexingEnabled(clazz, enabled);
    }

    @Override
    public Set<String> getAutoIndexedProperties(Class forClass) {
        return restAPI.getAutoIndexedProperties(forClass);
    }

    @Override
    public void startAutoIndexingProperty(Class forClass, String s) {
        restAPI.startAutoIndexingProperty(forClass, s);
    }

    @Override
    public void stopAutoIndexingProperty(Class forClass, String s) {
        restAPI.stopAutoIndexingProperty(forClass, s);
    }

    @Override
    public void delete(RestIndex index) {
        restAPI.delete(index);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key, Object value) {
        restAPI.removeFromIndex(index, entity, key, value);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key) {
        restAPI.removeFromIndex(index, entity, key);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity) {
        restAPI.removeFromIndex(index, entity);
    }


    @Override
    public <T extends PropertyContainer> void addToIndex(final T entity, final RestIndex index, final String key, final Object value) {
        if (!getTxManager().isActive()) {
            restAPI.addToIndex(entity, index, key, value);
            return;
        }
        getTxManager().getRemoteCypherTransaction().registerListener(new TransactionFinishListener() {
            @Override
            public void comitted() {
                restAPI.addToIndex(entity, index, key, value);
            }

            @Override
            public void rolledBack() {
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> T putIfAbsent(T entity, RestIndex index, String key, Object value) {
        return restAPI.putIfAbsent(entity, index, key, value);
    }

    @Override
    public boolean hasToUpdate(long lastUpdate) {
        return restAPI.hasToUpdate(lastUpdate);
    }

    @Override
    public IndexInfo indexInfo(final String indexType) {
        return restAPI.indexInfo(indexType);
    }


    @Override
    public Collection<String> getAllLabelNames() {
        return restAPI.getAllLabelNames();
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        return restAPI.getRelationshipTypes();
    }

    @Override
    public RestTraversalDescription createTraversalDescription() {
        return restAPI.createTraversalDescription();
    }

    public String getBaseUri() {
        return restAPI.getBaseUri();
    }

    @Override
    public RestEntityExtractor getEntityExtractor() {
        return restAPI.getEntityExtractor();
    }

    @Override
    public RestEntity createRestEntity(Map data) {
        return restAPI.createRestEntity(data);
    }


    public Iterable<Node> getAllNodes() {
        String statement = "MATCH (n) " + _QUERY_RETURN_NODE;
        Iterable<List<Object>> result = runQuery(statement, null).getRows();
        return new IterableWrapper<Node, List<Object>>(result) {
            @Override
            protected Node underlyingObjectToObject(List<Object> row) {
                return addToCache(toNode(row));
            }
        };
    }
}
