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

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.rest.graphdb.converter.RelationshipIterableConverter;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestEntityCache;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.IndexInfo;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.neo4j.rest.graphdb.query.CypherRestResult;
import org.neo4j.rest.graphdb.query.CypherResult;
import org.neo4j.rest.graphdb.query.RestQueryResult;
import org.neo4j.rest.graphdb.transaction.NullTransaction;
import org.neo4j.rest.graphdb.traversal.RestDirection;
import org.neo4j.rest.graphdb.traversal.RestTraversal;
import org.neo4j.rest.graphdb.traversal.RestTraversalDescription;
import org.neo4j.rest.graphdb.traversal.RestTraverser;
import org.neo4j.rest.graphdb.util.QueryResult;
import org.neo4j.rest.graphdb.util.ResultConverter;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.rest.graphdb.ExecutingRestRequest.encode;


public class RestAPIImpl implements RestAPI {

    public static final String _QUERY_RETURN_NODE = " RETURN id(n) as id, labels(n) as labels, n as data";
    public static final String GET_REL_TYPES_QUERY = "MATCH (n)-[r]-() WHERE id(n) = {id} RETURN distinct type(r) as relType";
    private static final String[] NO_LABELS = new String[0];
    protected RestRequest restRequest;

    private long entityRefetchTimeInMillis = TimeUnit.SECONDS.toMillis(1000); //TODO move to cache
    private final RestEntityCache entityCache = new RestEntityCache(this);
    private RestEntityExtractor restEntityExtractor = new RestEntityExtractor(this);

    private final RestAPIIndexImpl restIndexAPI;
    public RestAPIImpl(String uri) {
        this.restRequest = createRestRequest(uri, null, null);
        restIndexAPI = new RestAPIIndexImpl(this);
    }

    public RestAPIImpl(String uri, String user, String password) {
        this.restRequest = createRestRequest(uri, user, password);
        restIndexAPI = new RestAPIIndexImpl(this);
    }

    protected RestRequest createRestRequest(String uri, String user, String password) {
        return new ExecutingRestRequest(uri, user, password);
    }

    @Override
    public RestIndexManager index() {
        return restIndexAPI.index();
    }

    @Override
    public RestNode getNodeById(long id, Load force) {
        if (force != Load.ForceFromServer) {
            RestNode restNode = entityCache.getNode(id);
            if (restNode != null) return restNode;
        }
        if (force == Load.FromCache) return new RestNode(RestNode.nodeUri(this, id),this);

//        BatchRestAPI batchRestAPI = new BatchRestAPI(this);
//        RestNode node = batchRestAPI.getNodeById(id);
        RequestResult response = restRequest.get("node/" + id);
        if (response.statusIs(Status.NOT_FOUND)) {
            throw new NotFoundException("" + id);
        }
        RestNode node;
        Map<String, Object> data = (Map<String, Object>) response.toMap();
        if (response.isMap() && data.containsKey("metadata")) {
            node = new RestNode(data, this);
        } else {
            Collection<String> labels = getNodeLabels(id);
            node = new RestNode(id, labels, data, this);
        }
        return entityCache.addToCache(node);
    }

    @Override
    public RestRelationship getRelationshipById(long id, Load force) {
        if (force != Load.ForceFromServer) {
            RestRelationship restRel = entityCache.getRelationship(id);
            if (restRel != null) return restRel;
        }
        if (force == Load.FromCache) return new RestRelationship(RestRelationship.relUri(this, id),this);

//        BatchRestAPI batchRestAPI = new BatchRestAPI(this);
//        RestNode node = batchRestAPI.getNodeById(id);
        RequestResult response = restRequest.get("relationship/" + id);
        if (response.statusIs(Status.NOT_FOUND)) {
            throw new NotFoundException("" + id);
        }
        Map<String, Object> data = (Map<String, Object>) response.toMap();
        RestRelationship rel = new RestRelationship(data, this);
        return entityCache.addToCache(rel);
    }

    @Override
    public RestNode addToCache(RestNode restNode) {
        return entityCache.addToCache(restNode);
    }

    @Override
    public RestRelationship addToCache(RestRelationship rel) {
        return entityCache.addToCache(rel);
    }

    @Override
    public RestNode getNodeFromCache(long id) {
        return entityCache.getNode(id);
    }

    @Override
    public RestRelationship getRelFromCache(long id) {
        return entityCache.getRelationship(id);
    }

    @Override
    public void removeNodeFromCache(long id) {
        entityCache.removeNode(id);
    }
    @Override
    public void removeRelFromCache(long id) {
        entityCache.removeRelationship(id);
    }

    @Override
    public RestNode getNodeById(long id) {
        return getNodeById(id, Load.FromServer);
    }
    @Override
    public RestRelationship getRelationshipById(long id) {
        return getRelationshipById(id, Load.FromServer);
    }

    @Override
    public RestNode createNode(Map<String, Object> props) {
        return createNode(props,Collections.<String>emptyList());
    }

    @Override
    public RestNode createNode(Map<String, Object> props, Collection<String> labels) {
        RequestResult result = restRequest.post("node", props);
        RestNode node = createRestNode(result);
        if (node==null) {
            throw RestResultException.create(result);
        }
        addLabels(node,labels);
        node.setLabels(labels);
        return entityCache.addToCache(node);
    }

    private String toLabelString(String[] labels) {
        if (labels==null || labels.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String label : labels) {
            sb.append(":").append(label);
        }
        return sb.toString();
    }


    public RestNode createRestNode(RequestResult result) {
        if (result.statusIs(Status.NOT_FOUND)) {
            throw new NotFoundException("Node not found");
        }
        RestNode node = null;
        if (result.statusIs(CREATED)) {
            node = result.isMap() ? new RestNode(result.toMap(), this) : new RestNode(result.getLocation(), this);
        }
        if (node == null && result.statusIs(Status.OK)) {
            node = new RestNode(result.toMap(), this);
        }
        return entityCache.addToCache(node);
    }

    public RestRelationship createRestRelationship(RequestResult requestResult, PropertyContainer element) {
        if (requestResult.statusOtherThan(CREATED)) {
            final int status = requestResult.getStatus();
            throw new RuntimeException("Error creating relationship " + status+" "+requestResult.getText());
        }
        final String location = requestResult.getLocation();
        if (requestResult.isMap()) return new RestRelationship(requestResult.toMap(), this);
        return new RestRelationship(location, this);
    }

    @Override
    public RestRelationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props) {
        // final RestRequest restRequest = ((RestNode) startNode).getRestRequest();
        final RestNode end = (RestNode) endNode;
        Map<String, Object> data = map("to", end.getUri(), "type", type.name());
        if (props != null && props.size() > 0) {
            data.put("data", props);
        }
        final RestNode start = (RestNode) startNode;
        RequestResult requestResult = getRestRequest().with(start.getUri()).post("relationships", data);
        return createRestRelationship(requestResult, startNode);
    }

    @Override
    public void close() {
        ExecutingRestRequest.shutdown();
    }

    // TODO
    @Override
    public Relationship getOrCreateRelationship(Node start, Node end, RelationshipType type, Direction direction, Map<String, Object> props) {
        final Iterable<Relationship> existingRelationships = start.getRelationships(type, direction);
        for (final Relationship existingRelationship : existingRelationships) {
            if (existingRelationship != null && existingRelationship.getOtherNode(start).equals(end))
                return existingRelationship;
        }
        if (direction == Direction.INCOMING) {
            return createRelationship(end, start, type, props);
        } else {
            return createRelationship(start,end,type,props);
        }
    }

    protected Collection<Node> removeMissingRelationships(Node node, Collection<Node> targetNodes,
                                                          RelationshipType type, Direction direction, Label targetLabel) {
        targetNodes = new ArrayList<>(targetNodes);
        for (Relationship relationship : node.getRelationships( type, direction ) ) {
            Node otherNode = relationship.getOtherNode(node);
            if ( !targetNodes.remove(otherNode) ) {
                if (targetLabel != null && !relationship.getOtherNode(node).hasLabel(targetLabel)) continue;
                relationship.delete();
            }
        }
        return targetNodes;
    }
    @Override
    public Iterable<Relationship> updateRelationships(final Node start, Collection<Node> endNodes, final RelationshipType type, final Direction direction, String targetLabelName) {
        Label targetLabel = targetLabelName==null ? null : DynamicLabel.label(targetLabelName);
        Collection<Node> remainingEndNodes = removeMissingRelationships(start, endNodes, type, direction, targetLabel);
        List<Relationship> result=new ArrayList<>(remainingEndNodes.size());
        for (Node endNode : remainingEndNodes) {
            result.add(getOrCreateRelationship(start, endNode,type,direction,null));
        }
        return result;
    }

    @Override
    public RestNode merge(String labelName, String key, Object value, final Map<String, Object> nodeProperties, Collection<String> labels) {
        if (labelName ==null || key == null || value==null) throw new IllegalArgumentException("Label "+ labelName +" key "+key+" and value must not be null");
        Map props = nodeProperties.containsKey(key) ? nodeProperties : MapUtil.copyAndPut(nodeProperties, key, value);
        Map<String, Object> params = map("props", props, "value", value);
        Iterator<List<Object>> result = query(mergeQuery(labelName, key, labels), params).getData().iterator();
        if (!result.hasNext())
            throw new RuntimeException("Error merging node with labels: " + labelName + " key " + key + " value " + value + " labels " + labels+ " and props: " + props + " no data returned");

        return entityCache.addToCache(toNode(result.next()));
    }

    private String mergeQuery(String labelName, String key, Collection<String> labels) {
        StringBuilder setLabels = new StringBuilder();
        if (labels!=null) {
            for (String label : labels) {
                if (label.equals(labelName)) continue;
                setLabels.append("SET n:").append(label).append(" ");
            }
        }
        return "MERGE (n:`"+labelName+"` {`"+key+"`: {value}}) ON CREATE SET n={props} "+setLabels+ _QUERY_RETURN_NODE;
    }

    @Override
    public void removeLabel(RestNode node, String label) {
        RequestResult response = getRestRequest().with(node.getUri()).delete("labels/" + encode(label));
        if (response.statusOtherThan(Status.NO_CONTENT)) {
            throw new IllegalStateException("received " + response);
        }
    }

    public Collection<String> getNodeLabels(long id) {
        RequestResult response = restRequest.get(RestNode.nodeUri(this,id)+"/labels");
        if (response.statusOtherThan(Status.OK)) {
            throw new IllegalStateException("received " + response);
        }
        return (Collection<String>) response.toEntity();
    }

    @Override
    public Collection<String> getAllLabelNames() {
        RequestResult response = restRequest.get("labels");
        if (response.statusOtherThan(Status.OK)) {
            throw new IllegalStateException("received " + response);
        }
        return (Collection<String>) response.toEntity();
    }

    @Override
    public Iterable<RestNode> getNodesByLabel(String label) {
        RequestResult response = getRestRequest().get("label/" + encode(label) + "/nodes");
        if (response.statusOtherThan(Status.OK)) {
            throw new IllegalStateException("received " + response);
        }
        return (Iterable<RestNode>) getEntityExtractor().convertFromRepresentation(response);
    }

    private RestNode toNode(List<Object> row) {
        long id = ((Number) row.get(0)).longValue();
        List<String> labels = (List<String>) row.get(1);
        Map<String,Object> restData = (Map<String, Object>) row.get(2);
        return new RestNode(id, labels, restData, this);
    }

    private Iterable<RestNode> queryForNodes(String statement, Map<String, Object> params) {
        Iterable<List<Object>> result = query(statement, params).getData();
        return new IterableWrapper<RestNode,List<Object>>(result) {
            protected RestNode underlyingObjectToObject(List<Object> row) {
                return entityCache.addToCache(toNode(row));
            }
        };
    }

    @Override
    public Iterable<RestNode> getNodesByLabelAndProperty(String label, String property, Object value) {
        String statement = "MATCH (n:`" + label + "`) WHERE n.`"+property+"` = {value} " + _QUERY_RETURN_NODE;
        return queryForNodes(statement, map("value", value));
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes(RestNode node) {
        Iterable<List<Object>> result = query(GET_REL_TYPES_QUERY, map("id", node.getId())).getData();
        return new IterableWrapper<RelationshipType, List<Object>>(result) {
            protected RelationshipType underlyingObjectToObject(List<Object> row) {
                return DynamicRelationshipType.withName(row.get(0).toString());
            }
        };
    }

    @Override
    public int getDegree(RestNode restNode, RelationshipType type, Direction direction) {
        String relPattern = "--";
        if (type != null) relPattern = "-[:`"+type+"`]-";
        if (direction == Direction.OUTGOING) {
            relPattern += ">";
        } else if (direction == Direction.INCOMING) {
            relPattern = "<" + relPattern;
        }
        String nodeDegreeQuery = "MATCH (n)" + relPattern + "() WHERE id(n) = {id} RETURN count(*) as degree";
        Iterator<List<Object>> degree = query(nodeDegreeQuery, map("id", restNode.getId())).getData().iterator();
        if (!degree.hasNext()) return 0;
        return ((Number)degree.next().get(0)).intValue();
    }

    @Override
    public Iterable<RelationshipType> getRelationshipTypes() {
        Object result = restRequest.get("relationship/types").toEntity();
        if (!(result instanceof Iterable)) throw new RuntimeException("Error loading relationship types");

        return new IterableWrapper<RelationshipType, Object>((Iterable<Object>) result) {
            protected RelationshipType underlyingObjectToObject(Object type) {
                return DynamicRelationshipType.withName(type.toString());
            }
        };
    }

    @Override
    public void addLabels(RestNode node, Collection<String> labels) {
        if (labels == null || labels.isEmpty()) return;
        RequestResult response = getRestRequest().with(node.getUri()).post("labels", labels);

        if (response.statusOtherThan(Status.NO_CONTENT)) {
            throw new IllegalStateException("error adding labels, received " + response.getText());
        }
    }


    @Override
    public RestTraversalDescription createTraversalDescription() {
        return RestTraversal.description();
    }

    @Override
    public Transaction beginTx() {
        return new NullTransaction();
    }

    public long getEntityRefetchTimeInMillis() {
        return entityRefetchTimeInMillis;
    }

    public String getBaseUri() {
        return restRequest.getUri();
    }


    public void setEntityRefetchTimeInMillis(long entityRefetchTimeInMillis) {
        this.entityRefetchTimeInMillis = entityRefetchTimeInMillis;
    }

    @SuppressWarnings("unchecked")
    public Iterable<Relationship> wrapRelationships(RequestResult requestResult) {
        return (Iterable<Relationship>) new RelationshipIterableConverter(this).convertFromRepresentation(requestResult);
    }

    public RestEntityExtractor getEntityExtractor() {
        return restEntityExtractor;
    }


    @Override
    public void deleteEntity(RestEntity entity) {
        getRestRequest().with(entity.getUri()).delete( "" );
        entityCache.removeNode(entity.getId());
    }

    @Override
    public void setPropertyOnEntity(RestEntity entity, String key, Object value) {
        RequestResult result = getRestRequest().with(entity.getUri()).put("properties/" + encode(key), value);
        if (result.statusOtherThan(Status.NO_CONTENT))
            throw new RuntimeException("Error setting properties on entity "+entity+" properties "+key+" : "+value);
    }

    @Override
    public void setPropertiesOnEntity(RestEntity entity, Map<String,Object> properties) {
        RequestResult result = getRestRequest().with(entity.getUri()).put("properties", properties);
        if (result.statusOtherThan(Status.NO_CONTENT))
            throw new RuntimeException("Error setting properties on entity "+entity+" properties "+properties);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPropertiesFromEntity(RestEntity entity){
        RequestResult response = getRestRequest().with(entity.getUri()).get("properties");
        Map<String, Object> properties;
        boolean ok = response.statusIs( Status.OK );
        if ( ok ) {
            properties = (Map<String, Object>) response.toMap(  );
        } else {
            properties = Collections.emptyMap();
        }
       
        return properties;
    }

    @Override
    public boolean hasToUpdate(long lastUpdate) {
        return timeElapsed(lastUpdate, getEntityRefetchTimeInMillis());
    }

    @Override
    public void removeProperty(RestEntity entity, String key) {
        restRequest.with(entity.getUri()).delete("properties/" + encode(key));
    }

    private boolean timeElapsed( long since, long isItGreaterThanThis ) {
        return System.currentTimeMillis() - since > isItGreaterThanThis;
    }

    public org.neo4j.rest.graphdb.query.CypherResult query(String statement, Map<String, Object> params) {
        params =  (params==null) ? Collections.<String,Object>emptyMap() : params;
        final RequestResult requestResult = getRestRequest().post("cypher", map("query", statement, "params", params));
        return new CypherRestResult(requestResult);
    }

    @Override
    public Iterable<Relationship> getRelationships(RestNode restNode, Direction direction, RelationshipType... types) {
        String path = (types.length > 1) ? relPath(types) : relPath(direction,types.length == 0 ? null : types[0]);
        return wrapRelationships(getRestRequest().with(restNode.getUri()).get(path));
    }


    private String relPath(RelationshipType... types) {
        String path = "relationships/all/";
        int counter = 0;
        for ( RelationshipType type : types ) {
            if ( counter++ > 0 ) {
                path += "&";
            }
            path += encode(type.name());
        }
        return path;
    }

    private String relPath(Direction direction,RelationshipType type) {
        String path = "relationships/" + RestDirection.from(direction).shortName;
        return type == null ? path : path + "/" + encode(type.name());
    }


    private static final String FULLPATH = "fullpath";

    @Override
    public RestTraverser traverse(RestNode restNode, Map<String, Object> description) {
        final RequestResult result = getRestRequest().with(restNode.getUri()).post("traverse/" + FULLPATH, description);
        if (result.statusOtherThan(Response.Status.OK)) throw new RuntimeException(String.format("Error executing traversal: %d %s",result.getStatus(), description));
        final Object col = result.toEntity();
        if (!(col instanceof Collection)) throw new RuntimeException(String.format("Unexpected traversal result, %s instead of collection", col != null ? col.getClass() : null));
        return new RestTraverser((Collection) col,restNode.getRestApi());
    }

    public QueryResult<Map<String, Object>> query(String statement, Map<String, Object> params, ResultConverter resultConverter) {
        final CypherResult result = query(statement, params);
        if (RestResultException.isExceptionResult(result.asMap())) throw new RestResultException(result.asMap());
        return RestQueryResult.toQueryResult(result, this, resultConverter);
    }

    @Override
    public RestEntity createRestEntity(Map data) {
        if (data.containsKey("id") && data.containsKey("properties")) {
            long id = asLong(data, "id");
            Map props = (Map) data.get("properties");
            if (data.containsKey("type")) {
                return RestRelationship.fromCypher(id,(String)data.get("type"),props,asLong(data, "startNode"),asLong(data, "endNode"),this);
            }
            if (data.containsKey("labels")) {
                List<String> labels = (List<String>) data.get("labels");
                return entityCache.addToCache(RestNode.fromCypher(id,labels,props, this));
            }
        }
        final String uri = (String) data.get("self");
        if (uri == null || uri.isEmpty()) return null;
        if (uri.contains("/node/")) {
            return entityCache.addToCache(new RestNode(data, this));
        }
        if (uri.contains("/relationship/")) {
            return new RestRelationship(data, this);
        }
        return null;
    }

    @Override
    public RestRequest getRestRequest() {
        return restRequest;
    }

    protected long asLong(Map data, String idKey) {
        Object idValue = data.get(idKey);
        return idValue instanceof Number ? ((Number)idValue).longValue() : Long.parseLong(idValue.toString());
    }

    public RequestResult batch(Collection<Map<String, Object>> batchRequestData) {
        return restRequest.post("batch",batchRequestData);
    }

    @Override
    public IndexInfo indexInfo(String indexType) {
        return restIndexAPI.indexInfo(indexType);
    }

    @Override
    public void stopAutoIndexingProperty(Class forClass, String s) {
        restIndexAPI.stopAutoIndexingProperty(forClass, s);
    }

    @Override
    public void startAutoIndexingProperty(Class forClass, String s) {
        restIndexAPI.startAutoIndexingProperty(forClass, s);
    }

    @Override
    public Set<String> getAutoIndexedProperties(Class forClass) {
        return restIndexAPI.getAutoIndexedProperties(forClass);
    }

    @Override
    public void setAutoIndexingEnabled(Class<? extends PropertyContainer> clazz, boolean enabled) {
        restIndexAPI.setAutoIndexingEnabled(clazz, enabled);
    }

    @Override
    public boolean isAutoIndexingEnabled(Class<? extends PropertyContainer> clazz) {
        return restIndexAPI.isAutoIndexingEnabled(clazz);
    }

    @Override
    public <T extends PropertyContainer> RestIndex<T> createIndex(Class<T> type, String indexName, Map<String, String> config) {
        return restIndexAPI.createIndex(type, indexName, config);
    }

    @Override
    public void createIndex(String type, String indexName, Map<String, String> config) {
        restIndexAPI.createIndex(type, indexName, config);
    }

    @Override
    public <T extends PropertyContainer> RestIndex<T> getIndex(String indexName) {
        return restIndexAPI.getIndex(indexName);
    }

    @Override
    public RestRelationship getOrCreateRelationship(RestIndex<Relationship> index, String key, Object value, RestNode start, RestNode end, String type, Map<String, Object> properties) {
        return restIndexAPI.getOrCreateRelationship(index, key, value, start, end, type, properties);
    }

    @Override
    public RestNode getOrCreateNode(RestIndex<Node> index, String key, Object value, Map<String, Object> properties, Collection<String> labels) {
        return restIndexAPI.getOrCreateNode(index, key, value, properties, labels);
    }

    @Override
    public <T extends PropertyContainer> T putIfAbsent(T entity, RestIndex index, String key, Object value) {
        return restIndexAPI.putIfAbsent(entity, index, key, value);
    }

    @Override
    public <T extends PropertyContainer> void addToIndex(T entity, RestIndex index, String key, Object value) {
        restIndexAPI.addToIndex(entity, index, key, value);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity) {
        restIndexAPI.removeFromIndex(index, entity);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key) {
        restIndexAPI.removeFromIndex(index, entity, key);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key, Object value) {
        restIndexAPI.removeFromIndex(index, entity, key, value);
    }

    @Override
    public void delete(RestIndex index) {
        restIndexAPI.delete(index);
    }

    @Override
    public <S extends PropertyContainer> IndexHits<S> queryIndex(Class<S> entityType, String indexName, String key, Object value) {
        return restIndexAPI.queryIndex(entityType, indexName, key, value);
    }

    @Override
    public <S extends PropertyContainer> IndexHits<S> getIndex(Class<S> entityType, String indexName, String key, Object value) {
        return restIndexAPI.getIndex(entityType, indexName, key, value);
    }

}
