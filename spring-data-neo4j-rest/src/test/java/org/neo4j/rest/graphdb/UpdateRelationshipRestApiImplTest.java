package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.GraphDatabaseService;

/**
 * @author mh
 * @since 30.09.14
 */
public class UpdateRelationshipRestApiImplTest extends UpdateRelationshipTest {

    @Override
    protected GraphDatabaseService createRestGraphDatabase() {
        restAPI = new RestAPIImpl(SERVER_ROOT_URI);
        return new RestGraphDatabase(restAPI);
    }
}
