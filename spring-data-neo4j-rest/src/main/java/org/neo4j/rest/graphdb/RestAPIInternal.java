package org.neo4j.rest.graphdb;

import org.neo4j.rest.graphdb.converter.RestEntityExtractor;
import org.neo4j.rest.graphdb.entity.RestEntity;
import org.neo4j.rest.graphdb.entity.RestNode;

import java.util.Map;

/**
 * @author mh
 * @since 21.09.14
 */
public interface RestAPIInternal {
    RestNode getNodeById(long id, RestAPI.Load force);

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
}
