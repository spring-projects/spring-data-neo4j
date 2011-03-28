package org.neo4j.rest.graphdb;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.event.KernelEventHandler;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.Config;
import org.neo4j.kernel.RestConfig;
import org.neo4j.rest.graphdb.index.RestIndexManager;

import javax.ws.rs.core.Response.Status;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

public class RestGraphDatabase extends AbstractGraphDatabase {
    private RestRequest restRequest;
    private long propertyRefetchTimeInMillis = 1000;


    public RestGraphDatabase( URI uri ) {
        restRequest = new RestRequest( uri );
    }

    public RestGraphDatabase( URI uri, String user, String password ) {
        restRequest = new RestRequest( uri, user, password );
    }

    public Transaction beginTx() {
        return new Transaction() {
            public void success() {
            }

            public void finish() {

            }

            public void failure() {
            }
        };
    }

    public <T> TransactionEventHandler<T> registerTransactionEventHandler( TransactionEventHandler<T> tTransactionEventHandler ) {
        throw new UnsupportedOperationException();
    }

    public <T> TransactionEventHandler<T> unregisterTransactionEventHandler( TransactionEventHandler<T> tTransactionEventHandler ) {
        throw new UnsupportedOperationException();
    }

    public KernelEventHandler registerKernelEventHandler( KernelEventHandler kernelEventHandler ) {
        throw new UnsupportedOperationException();
    }

    public KernelEventHandler unregisterKernelEventHandler( KernelEventHandler kernelEventHandler ) {
        throw new UnsupportedOperationException();
    }

    public IndexManager index() {
        return new RestIndexManager( restRequest, this );
    }

    public Node createNode() {
        ClientResponse response = restRequest.post( "node", null );
        if ( restRequest.statusOtherThan( response, Status.CREATED ) ) {
            throw new RuntimeException( "" + response.getStatus() );
        }
        return new RestNode( response.getLocation(), this );
    }

    public boolean enableRemoteShell() {
        throw new UnsupportedOperationException();
    }

    public boolean enableRemoteShell( Map<String, Serializable> config ) {
        throw new UnsupportedOperationException();
    }

    public Iterable<Node> getAllNodes() {
        throw new UnsupportedOperationException();
    }

    public Node getNodeById( long id ) {
        ClientResponse response = restRequest.get( "node/" + id );
        if ( restRequest.statusIs( response, Status.NOT_FOUND ) ) {
            throw new NotFoundException( "" + id );
        }
        return new RestNode( restRequest.toMap( response ), this );
    }

    public Node getReferenceNode() {
        Map<?, ?> map = restRequest.toMap( restRequest.get( "" ) );
        return new RestNode( (String) map.get( "reference_node" ), this );
    }

    public Relationship getRelationshipById( long id ) {
        ClientResponse response = restRequest.get( "relationship/" + id );
        if ( restRequest.statusIs( response, Status.NOT_FOUND ) ) {
            throw new NotFoundException( "" + id );
        }
        return new RestRelationship( restRequest.toMap( response ), this );
    }

    public Iterable<RelationshipType> getRelationshipTypes() {
        throw new UnsupportedOperationException();
    }

    public void shutdown() {
    }

    public RestRequest getRestRequest() {
        return restRequest;
    }

    public long getPropertyRefetchTimeInMillis() {
        return propertyRefetchTimeInMillis;
	}
    @Override
    public String getStoreDir() {
        return restRequest.getUri().toString();
    }

    @Override
    public Config getConfig() {
        return new RestConfig(this);
    }

    @Override
    public <T> T getManagementBean(Class<T> type) {
        return null;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
