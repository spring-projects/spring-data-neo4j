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

package org.neo4j.rest.graphdb.index;

import com.sun.jersey.api.client.ClientResponse;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.RestRequest;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class RestIndexManager implements IndexManager {
    private RestRequest restRequest;
    private RestGraphDatabase restGraphDatabase;

    public RestIndexManager( RestRequest restRequest, RestGraphDatabase restGraphDatabase ) {
        this.restRequest = restRequest;
        this.restGraphDatabase = restGraphDatabase;
    }

    public boolean existsForNodes( String indexName ) {
        return indexInfo( "node" ).containsKey( indexName );
    }

    private Map<String, ?> indexInfo( final String indexType ) {
        ClientResponse response = restRequest.get( "index/" + indexType );
        if ( restRequest.statusIs( response, ClientResponse.Status.NO_CONTENT ) ) return Collections.emptyMap();
        return (Map<String, ?>) restRequest.toMap( response );
    }

    public Index<Node> forNodes( String indexName ) {
        return new RestNodeIndex( restRequest, indexName, restGraphDatabase );
    }

    public Index<Node> forNodes( String indexName, Map<String, String> stringStringMap ) {
        return new RestNodeIndex( restRequest, indexName, restGraphDatabase );
    }

    public String[] nodeIndexNames() {
        Set<String> keys = indexInfo( "node" ).keySet();
        return keys.toArray( new String[keys.size()] );
    }

    public boolean existsForRelationships( String indexName ) {
        return indexInfo( "relationship" ).containsKey( indexName );
    }

    public RelationshipIndex forRelationships( String indexName ) {
        return new RestRelationshipIndex( restRequest, indexName, restGraphDatabase );
    }

    public RelationshipIndex forRelationships( String indexName, Map<String, String> stringStringMap ) {
        return new RestRelationshipIndex( restRequest, indexName, restGraphDatabase );
    }

    public String[] relationshipIndexNames() {
        Set<String> keys = indexInfo( "relationship" ).keySet();
        return keys.toArray( new String[keys.size()] );
    }

    public Map<String, String> getConfiguration( Index<? extends PropertyContainer> index ) {
        return null;
    }

    public String setConfiguration( Index<? extends PropertyContainer> index, String s, String s1 ) {
        return null;
    }

    public String removeConfiguration( Index<? extends PropertyContainer> index, String s ) {
        return null;
    }
}

