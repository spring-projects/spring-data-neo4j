package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.ValueContext;
import org.neo4j.rest.graphdb.converter.RestIndexHitsConverter;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.*;
import org.neo4j.rest.graphdb.util.JsonHelper;

import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.neo4j.helpers.collection.MapUtil.map;
import static org.neo4j.rest.graphdb.ExecutingRestRequest.encode;

/**
 * @author mh
 * @since 25.08.15
 */
public class RestAPIIndexImpl implements RestAPIIndex {
    private final RestRequest restRequest;
    private RestIndexManager restIndexManager;
    private Map<String,IndexInfo> indexInfos = new HashMap<>();
    private final RestAPI restAPI;

    public RestAPIIndexImpl(RestAPI restAPI) {
        this.restAPI = restAPI;
        restRequest = restAPI.getRestRequest();
        restIndexManager = new RestIndexManager(restAPI);
    }

    @Override
    public RestIndexManager index() {
        return restIndexManager;
    }

    public static String indexPath( Class entityType, String indexName ) {
        return "index/" + indexTypeName(entityType) + "/" + encode(indexName);
    }

    public static String queryPath(  Class entityType, String indexName, String key, Object value ) {
        return indexPath( entityType, indexName, key,null) + "?query="+ encode(value);
    }

    public static String indexPath( Class entityType, String indexName, String key, Object value ) {
        String typeName = indexTypeName(entityType);
        return "index/" + typeName + "/" + encode(indexName) + (key!=null? "/" + encode(key) :"") + (value!=null ? "/" + encode(value):"");
    }

    public static String indexTypeName(Class entityType) {
        return entityType.getSimpleName().toLowerCase();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> IndexHits<S> getIndex(Class<S> entityType, String indexName, String key, Object value) {
        String indexPath = indexPath(entityType, indexName, key, value);
        RequestResult response = restRequest.get(indexPath);
        if (response.statusIs(Response.Status.OK)) {
            return new RestIndexHitsConverter(restAPI, entityType).convertFromRepresentation(response);
        } else {
            return new SimpleIndexHits<S>(Collections.emptyList(), 0, entityType, restAPI);
        }
    }
    @Override
    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> IndexHits<S> queryIndex(Class<S> entityType, String indexName, String key, Object value) {
        String indexPath = queryPath(entityType, indexName, key, value);
        RequestResult response = restRequest.get(indexPath);
        if (response.statusIs(Response.Status.OK)) {
            return new RestIndexHitsConverter(restAPI, entityType).convertFromRepresentation(response);
        } else {
            return new SimpleIndexHits<S>(Collections.emptyList(), 0, entityType, restAPI);
        }
    }
    private void deleteIndex(String indexPath) {
        getRestRequest().delete(indexPath);
    }

    @Override
    public void delete(RestIndex index) {
        deleteIndex(indexPath(index, null, null));
        resetIndex(index.getEntityType());
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key, Object value) {
        String indexPath = indexPath(index, key, value);
        deleteIndex(indexPath(indexPath, entity));
        resetIndex(index.getEntityType());
    }

    protected <T extends PropertyContainer> String indexPath(String indexPath, T restEntity) {
        return indexPath + "/" + ((RestEntity)restEntity).getId();
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key) {
        String indexPath = indexPath(index, key, null);
        deleteIndex(indexPath(indexPath, entity));
        resetIndex(index.getEntityType());
    }

    private String indexPath(RestIndex index, String key, Object value) {
        return indexPath(index.getEntityType(), index.getIndexName(), key, value);
    }

    @Override
    public <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity) {
        deleteIndex(indexPath(indexPath(index, null, null), entity));
        resetIndex(index.getEntityType());
    }

    public String uniqueIndexPath(RestIndex index) {
        return indexPath(index,null,null) + "?uniqueness=get_or_create";
    }

    @Override
    public <T extends PropertyContainer> void addToIndex(T entity, RestIndex index, String key, Object value) {
        final RestEntity restEntity = (RestEntity) entity;
        String uri = restEntity.getUri();
        if (value instanceof ValueContext) {
            value = ((ValueContext)value).getCorrectValue();
        }
        final Map<String, Object> data = map("key", key, "value", value, "uri", uri);
        final RequestResult result = getRestRequest().post(indexPath(index, null, null), data);
        if (result.statusOtherThan(Response.Status.CREATED)) {
            throw new RuntimeException(String.format("Error adding element %d %s %s to index %s status %s\n%s", restEntity.getId(), key, value, index.getIndexName(), result.getStatus(),result.getText()));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> T putIfAbsent(T entity, RestIndex index, String key, Object value) {
        final RestEntity restEntity = (RestEntity) entity;
        restEntity.flush();
        String uri = restEntity.getUri();
        if (value instanceof ValueContext) {
            value = ((ValueContext)value).getCorrectValue();
        }
        final Map<String, Object> data = map("key", key, "value", value, "uri", uri);
        final RequestResult result = getRestRequest().post(uniqueIndexPath(index), data);
        if (result.statusIs(Response.Status.CREATED)) {
            if (index.getEntityType().equals(Node.class)) return (T)createRestNode(result);
            if (index.getEntityType().equals(Relationship.class)) return (T)createRestRelationship(result, restEntity);
        }
        if (result.statusIs(Response.Status.OK)) {
            return (T) restAPI.getEntityExtractor().convertFromRepresentation(result);
        }
        throw new RuntimeException(String.format("Error adding element %d %s %s to index %s", restEntity.getId(), key, value, index.getIndexName()));
    }

    public RestNode createRestNode(RequestResult result) {
        if (result.statusIs(Response.Status.NOT_FOUND)) {
            throw new NotFoundException("Node not found");
        }
        RestNode node = null;
        if (result.statusIs(CREATED)) {
            node = result.isMap() ? new RestNode(result.toMap(), restAPI) : new RestNode(result.getLocation(), restAPI);
        }
        if (node == null && result.statusIs(Response.Status.OK)) {
            node = new RestNode(result.toMap(), restAPI);
        }
        return restAPI.addToCache(node);
    }

    public RestRelationship createRestRelationship(RequestResult requestResult, PropertyContainer element) {
        if (requestResult.statusOtherThan(CREATED)) {
            final int status = requestResult.getStatus();
            throw new RuntimeException("Error creating relationship " + status+" "+requestResult.getText());
        }
        final String location = requestResult.getLocation();
        if (requestResult.isMap()) return new RestRelationship(requestResult.toMap(), restAPI);
        return new RestRelationship(location, restAPI);
    }

    @Override
    public RestNode getOrCreateNode(RestIndex<Node> index, String key, Object value, final Map<String, Object> properties, Collection<String> labels) {
        if (index==null || key == null || value==null) throw new IllegalArgumentException("Unique index "+index+" key "+key+" value must not be null");
        final Map<String, Object> data = map("key", key, "value", value, "properties", properties);
        final RequestResult result = getRestRequest().post(uniqueIndexPath(index), data);
        if (result.statusIs(Response.Status.CREATED) || result.statusIs(Response.Status.OK)) {
            RestNode node = (RestNode) restAPI.getEntityExtractor().convertFromRepresentation(result);
            restAPI.addLabels(node, labels);
            node.setLabels(labels);
            return restAPI.addToCache(node);
        }
        throw new RuntimeException(String.format("Error retrieving or creating node for key %s and value %s with index %s", key, value, index.getIndexName()));
    }

    @Override
    public RestRelationship getOrCreateRelationship(RestIndex<Relationship> index, String key, Object value, final RestNode start, final RestNode end, final String type, final Map<String, Object> properties) {
        if (index==null || key == null || value==null) throw new IllegalArgumentException("Unique index "+index+" key "+key+" value must not be null");
        if (start == null || end == null || type == null) throw new IllegalArgumentException("Neither start, end nore type must be null");
        final Map<String, Object> data = map("key", key, "value", value, "properties", properties, "start", start.getUri(), "end", end.getUri(), "type", type);
        final RequestResult result = getRestRequest().post(uniqueIndexPath(index), data);
        if (result.statusIs(Response.Status.CREATED) || result.statusIs(Response.Status.OK)) {
            return (RestRelationship) restAPI.getEntityExtractor().convertFromRepresentation(result);
        }
        throw new RuntimeException(String.format("Error retrieving or creating relationship for key %s and value %s with index %s", key, value, index.getIndexName()));
    }

    private String buildPathAutoIndexerStatus(Class<? extends PropertyContainer> clazz) {
        return buildPathAutoIndexerBase(clazz).append("/status").toString();
    }

    private StringBuilder buildPathAutoIndexerProperties(Class<? extends PropertyContainer> clazz) {
        return buildPathAutoIndexerBase(clazz).append("/properties");
    }

    private StringBuilder buildPathAutoIndexerBase(Class<? extends PropertyContainer> clazz) {
        return new StringBuilder().append("index/auto/").append(indexTypeName(clazz));
    }


    public RestRequest getRestRequest() {
        return restRequest;
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
        Map<String,Object> data=new HashMap<String, Object>();
        data.put("name",indexName);
        data.put("config",config);
        restRequest.post("index/" + type, data);
        IndexInfo indexInfo = indexInfos.get(type);
        if (indexInfo!=null) indexInfo.setExpired();
    }

    public void resetIndex(Class type) {
        if (Node.class.isAssignableFrom(type)) {
            indexInfo(RestIndexManager.NODE).setExpired();
        }
        if (Relationship.class.isAssignableFrom(type)) {
            indexInfo(RestIndexManager.RELATIONSHIP).setExpired();
        }
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
    public boolean isAutoIndexingEnabled(Class<? extends PropertyContainer> clazz) {
        RequestResult response = getRestRequest().get(buildPathAutoIndexerStatus(clazz));
        if (response.statusIs(Response.Status.OK)) {
            return Boolean.parseBoolean(response.getText());
        } else {
            throw new IllegalStateException("received " + response);
        }
    }

    @Override
    public void setAutoIndexingEnabled(Class<? extends PropertyContainer> clazz, boolean enabled) {
        RequestResult response = getRestRequest().put(buildPathAutoIndexerStatus(clazz), enabled);
        if (response.statusOtherThan(Response.Status.NO_CONTENT)) {
            throw new IllegalStateException("received " + response);
        }
    }

    @Override
    public Set<String> getAutoIndexedProperties(Class forClass) {
        RequestResult response = getRestRequest().get(buildPathAutoIndexerProperties(forClass).toString());
        Collection<String> autoIndexedProperties = (Collection<String>) JsonHelper.readJson(response.getText());
        return new HashSet<String>(autoIndexedProperties);
    }

    @Override
    public void startAutoIndexingProperty(Class forClass, String s) {
        try {
            // we need to use a inputstream instead of the string directly. Otherwise "post" implicitly uses
            // StreamJsonHelper.writeJsonTo which quotes a given string
            InputStream stream = new ByteArrayInputStream(s.getBytes("UTF-8"));
            RequestResult response = getRestRequest().post(buildPathAutoIndexerProperties(forClass).toString(), stream);
            if (response.statusOtherThan(Response.Status.NO_CONTENT)) {
                throw new IllegalStateException("received " + response);
            }
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }

    }

    @Override
    public void stopAutoIndexingProperty(Class forClass, String s) {
        RequestResult response = getRestRequest().delete(buildPathAutoIndexerProperties(forClass).append("/").append(s).toString());
        if (response.statusOtherThan(Response.Status.NO_CONTENT)) {
            throw new IllegalStateException("received " + response);
        }
    }

    @Override
    public IndexInfo indexInfo(final String indexType) {
        IndexInfo indexInfo = indexInfos.get(indexType);
        if (indexInfo != null && !indexInfo.isExpired()) {
            return indexInfo;
        }
        RequestResult response = restRequest.get("index/" + encode(indexType));
        indexInfo = new RetrievedIndexInfo(response);
        indexInfos.put(indexType,indexInfo);
        return indexInfo;
    }
}
