package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.entity.RestRelationship;

import java.util.Map;

/**
 * @author mh
 * @since 21.09.14
 */
public interface RestAPIInternal {
    RestNode getNodeById(long id, RestAPI.Load force);
    RestRelationship getRelationshipById(long id, RestAPI.Load force);

    boolean hasToUpdate(long lastUpdate);

    String getBaseUri();

    RestEntityExtractor getEntityExtractor();

    // todo add to cache or update data in cache
    RestEntity createRestEntity(Map data);

    public enum Load {
        FromCache,
        FromServer,
        ForceFromServer
    }

    RestRequest getRestRequest();

    RestNode addToCache(RestNode restNode);
    RestRelationship addToCache(RestRelationship restRelationship);
    RestNode getNodeFromCache(long id);
    RestRelationship getRelFromCache(long id);
    void removeNodeFromCache(long id);
    void removeRelFromCache(long id);
}
