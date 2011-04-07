/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.rest.support;

import org.neo4j.graphdb.*;

import java.net.URI;
import java.util.Map;

public class RestRelationship extends RestEntity implements Relationship {

    RestRelationship( URI uri, RestGraphDatabase graphDatabaseService ) {
        super( uri, graphDatabaseService );
    }

    RestRelationship( String uri, RestGraphDatabase graphDatabase ) {
        super( uri, graphDatabase );
    }

    public RestRelationship( Map<?, ?> data, RestGraphDatabase graphDatabase ) {
        super( data, graphDatabase );
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
        return new RestNode( uri, getRestGraphDatabase() );
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
}
