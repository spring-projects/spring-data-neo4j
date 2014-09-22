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
package org.neo4j.rest.graphdb.entity;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestAPIInternal;

import static org.neo4j.helpers.collection.MapUtil.map;


public class RestRelationship extends RestEntity implements Relationship {

    RestRelationship( URI uri, RestAPI restApi ) {
        super( uri, restApi );
    }

    public RestRelationship( String uri, RestAPI restApi ) {
        super( uri, restApi );
    }    
  
    public RestRelationship( Map<?, ?> data, RestAPI restApi ) {
        super( data, restApi );
    }

    @Override
    protected void doUpdate() {
        updateFrom(restApi.getRelationshipById(getId()), restApi);
    }

    public Node getEndNode() {
        return node( (String) getStructuralData().get( "end" ) );
    }

    public Node[] getNodes() {
        return new Node[]{
                node( (String) getStructuralData().get( "start" ) ),
                node( (String) getStructuralData().get( "end" ) )
        };
    }

    public static RestRelationship fromCypher(long id, String type, Map<String, Object> props, long start, long end, RestAPI facade) {
        Map<String, Object> restData = map("data", props, "self", relUri(facade, id), "start", RestNode.nodeUri(facade, start), "end", RestNode.nodeUri(facade,end), "type",type);
        return new RestRelationship(restData, facade);
    }


    public Node getOtherNode( Node node ) {
        long nodeId = node.getId();
        String startNodeUri = (String) getStructuralData().get( "start" );
        String endNodeUri = (String) getStructuralData().get( "end" );
        if ( getEntityId( startNodeUri ) == nodeId ) {
            return node( endNodeUri );
        } else if ( getEntityId( endNodeUri ) == nodeId ) {
            return node( startNodeUri );
        } else {
            throw new NotFoundException( node + " isn't one of start/end for " + this );
        }
    }

    private RestNode node( String uri ) {
        return getRestApi().getNodeById(getEntityId(uri), RestAPIInternal.Load.FromCache);
    }

    public static String relUri(RestAPI facade, long id) {
        return facade.getBaseUri()+"/relationship/" + id;
    }

    public Node getStartNode() {
        return node( (String) getStructuralData().get( "start" ) );
    }

    public RelationshipType getType() {
        return DynamicRelationshipType.withName( (String) getStructuralData().get( "type" ) );
    }

    public boolean isType( RelationshipType type ) {
        return type.name().equals( getStructuralData().get( "type" ) );
    }
    
    
    public RestRelationship create(RestNode startNode, RestNode endNode, RelationshipType type, Map<String, Object> props) {
       return this.restApi.createRelationship(startNode, endNode, type, props);
    }
   
}
