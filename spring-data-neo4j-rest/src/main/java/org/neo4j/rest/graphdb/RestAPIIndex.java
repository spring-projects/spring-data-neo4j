package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;
import org.neo4j.rest.graphdb.index.IndexInfo;
import org.neo4j.rest.graphdb.index.RestIndex;
import org.neo4j.rest.graphdb.index.RestIndexManager;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author mh
 * @since 21.09.14
 */
public interface RestAPIIndex {
    RestIndexManager index();

    @SuppressWarnings("unchecked")
    <T extends PropertyContainer> RestIndex<T> getIndex(String indexName);

    @SuppressWarnings("unchecked")
    void createIndex(String type, String indexName, Map<String, String> config);

    @SuppressWarnings("unchecked")
    <S extends PropertyContainer> IndexHits<S> getIndex(Class<S> entityType, String indexName, String key, Object value);
    <S extends PropertyContainer> IndexHits<S> queryIndex(Class<S> entityType, String indexName, String key, Object value);

    IndexInfo indexInfo(String indexType);

    <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key, Object value);

    <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity, String key);

    <T extends PropertyContainer> void removeFromIndex(RestIndex index, T entity);

    <T extends PropertyContainer> void addToIndex(T entity, RestIndex index, String key, Object value);

    @SuppressWarnings("unchecked")
    <T extends PropertyContainer> T putIfAbsent(T entity, RestIndex index, String key, Object value);

    RestNode getOrCreateNode(RestIndex<Node> index, String key, Object value, Map<String, Object> properties, Collection<String> labels);

    RestRelationship getOrCreateRelationship(RestIndex<Relationship> index, String key, Object value, RestNode start, RestNode end, String type, Map<String, Object> properties);

    @SuppressWarnings("unchecked")
    <T extends PropertyContainer> RestIndex<T> createIndex(Class<T> type, String indexName, Map<String, String> config);

    boolean isAutoIndexingEnabled(Class<? extends PropertyContainer> clazz);

    void setAutoIndexingEnabled(Class<? extends PropertyContainer> clazz, boolean enabled);

    Set<String> getAutoIndexedProperties(Class forClass);

    void startAutoIndexingProperty(Class forClass, String s);

    void stopAutoIndexingProperty(Class forClass, String s);

    void delete(RestIndex index);
}
