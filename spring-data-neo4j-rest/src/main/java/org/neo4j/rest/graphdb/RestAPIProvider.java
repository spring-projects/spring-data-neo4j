package org.neo4j.rest.graphdb;

/**
 * @author mh
 * @since 11.11.14
 */
public interface RestAPIProvider {
    RestAPI getRestAPI();
}
