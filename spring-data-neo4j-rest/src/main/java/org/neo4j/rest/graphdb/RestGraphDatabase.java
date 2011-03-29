package org.neo4j.rest.graphdb;

import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.rest.graphdb.index.RestIndexManager;
import org.springframework.data.graph.core.GraphDatabase;

import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.Map;

public class RestGraphDatabase implements GraphDatabase {
    private RestRequest restRequest;
    private long propertyRefetchTimeInMillis = 1000;


    public RestGraphDatabase( URI uri ) {
        restRequest = new RestRequest( uri );
    }

    public RestGraphDatabase( URI uri, String user, String password ) {
        restRequest = new RestRequest( uri, user, password );
    }

    @Override
    public Node getNode(long id) {
        ClientResponse response = restRequest.get("node/" + id);
        if ( restRequest.statusIs(response, Status.NOT_FOUND) ) {
            throw new NotFoundException( "" + id );
        }
        return new RestNode( restRequest.toMap(response), this );
    }

    @Override
    public Node createNode(Map<String, Object> props, String... indexFields) {
        ClientResponse response = restRequest.post("node", null);
        if ( restRequest.statusOtherThan( response, Status.CREATED ) ) {
            throw new RuntimeException( "" + response.getStatus() );
        }
        return new RestNode( response.getLocation(), this );
    }

    @Override
    public Relationship getRelationship(long id) {
        ClientResponse response = restRequest.get( "relationship/" + id );
        if ( restRequest.statusIs( response, Status.NOT_FOUND ) ) {
            throw new NotFoundException( "" + id );
        }
        return new RestRelationship( restRequest.toMap( response ), this );
    }

    @Override
    public Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props, String... indexFields) {
        Relationship relationship = startNode.createRelationshipTo(endNode, type);
        return relationship;
    }

    @Override
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        return (Index<T>) index().forNodes(indexName); // todo
    }

    @Override
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
        return (Index<T>) index().forNodes(indexName); // todo
    }

    @Override
    public TraversalDescription createTraversalDescription() {
        return new RestTraversal();
    }

    private RestIndexManager index() {
        return new RestIndexManager( restRequest, this );
    }

    @Override
    public Node getReferenceNode() {
        Map<?, ?> map = restRequest.toMap( restRequest.get( "" ) );
        return new RestNode( (String) map.get( "reference_node" ), this );
    }

    public RestRequest getRestRequest() {
        return restRequest;
    }

    public long getPropertyRefetchTimeInMillis() {
        return propertyRefetchTimeInMillis;
	}
}
