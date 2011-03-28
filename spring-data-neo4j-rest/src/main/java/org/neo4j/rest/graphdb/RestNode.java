package org.neo4j.rest.graphdb;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.helpers.collection.MapUtil;

import javax.ws.rs.core.Response.Status;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

public class RestNode extends RestEntity implements Node {
    public RestNode( URI uri, RestGraphDatabase graphDatabase ) {
        super( uri, graphDatabase );
    }

    public RestNode( String uri, RestGraphDatabase graphDatabase ) {
        super( uri, graphDatabase );
    }

    public RestNode( Map<?, ?> data, RestGraphDatabase graphDatabase ) {
        super( data, graphDatabase );
    }

    public Relationship createRelationshipTo( Node toNode, RelationshipType type ) {
        Map<String, Object> data = MapUtil.map( "to", ( (RestNode) toNode ).getUri(),
                "type", type.name() );

        ClientResponse response = restRequest.post( "relationships", JsonHelper.createJsonFrom( data ) );
        if ( restRequest.statusOtherThan( response, Status.CREATED ) ) {
            throw new RuntimeException( "" + response.getStatus() );
        }
        return new RestRelationship( response.getLocation(), getGraphDatabase() );
    }

    public Iterable<Relationship> getRelationships() {
        return wrapRelationships( restRequest.get( "relationships/all" ) );
    }

    @SuppressWarnings("unchecked")
    private Iterable<Relationship> wrapRelationships( ClientResponse response ) {
        return new IterableWrapper<Relationship, Object>(
                (Collection<Object>) restRequest.toEntity( response ) ) {
            @Override
            protected Relationship underlyingObjectToObject( Object data ) {
                return new RestRelationship( (Map<?, ?>) data, getGraphDatabase() );
            }
        };
    }

    public Iterable<Relationship> getRelationships( RelationshipType... types ) {
        String path = getStructuralData().get( "all_relationships" ) + "/";
        int counter = 0;
        for ( RelationshipType type : types ) {
            if ( counter++ > 0 ) {
                path += "&";
            }
            path += type.name();
        }
        return wrapRelationships( restRequest.get( path ) );
    }


    enum RestDirection {
        INCOMING( Direction.INCOMING, "incoming", "in" ),
        OUTGOING( Direction.OUTGOING, "outgoing", "out" ),
        BOTH( Direction.BOTH, "all", "all" );

        public final Direction direction;
        public final String dataName;
        public final String pathName;

        RestDirection( Direction direction, String dataName, String pathName ) {
            this.direction = direction;
            this.dataName = dataName;
            this.pathName = pathName;
        }

        static RestDirection from( Direction direction ) {
            for ( RestDirection restDirection : values() ) {
                if ( restDirection.direction == direction ) return restDirection;
            }
            throw new RuntimeException( "No Rest-Direction for " + direction );
        }
    }

    public Iterable<Relationship> getRelationships( Direction direction ) {
        return wrapRelationships( restRequest.get( "relationships/" + RestDirection.from( direction ).pathName ) );
    }

    public Iterable<Relationship> getRelationships( RelationshipType type,
                                                    Direction direction ) {
        String relationshipsKey = RestDirection.from( direction ).dataName + "_relationships";
        Object relationship = getStructuralData().get( relationshipsKey );
        return wrapRelationships( restRequest.get( relationship + "/" + type.name() ) );
    }

    public Relationship getSingleRelationship( RelationshipType type,
                                               Direction direction ) {
        return IteratorUtil.singleOrNull( getRelationships( type, direction ) );
    }

    public boolean hasRelationship() {
        return getRelationships().iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType... types ) {
        return getRelationships( types ).iterator().hasNext();
    }

    public boolean hasRelationship( Direction direction ) {
        return getRelationships( direction ).iterator().hasNext();
    }

    public boolean hasRelationship( RelationshipType type, Direction direction ) {
        return getRelationships( type, direction ).iterator().hasNext();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, Object... rels ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction ) {
        throw new UnsupportedOperationException();
    }

    public Traverser traverse( Order order, StopEvaluator stopEvaluator,
                               ReturnableEvaluator returnableEvaluator, RelationshipType type, Direction direction,
                               RelationshipType secondType, Direction secondDirection ) {
        throw new UnsupportedOperationException();
    }
}
