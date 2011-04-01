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
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.rest.graphdb.JsonHelper;
import org.neo4j.rest.graphdb.RestEntity;
import org.neo4j.rest.graphdb.RestGraphDatabase;
import org.neo4j.rest.graphdb.RestRequest;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * @author mh
 * @since 24.01.11
 */
public abstract class RestIndex<T extends PropertyContainer> implements Index<T> {
    protected final RestRequest restRequest;
    private final String indexName;
    protected final RestGraphDatabase restGraphDatabase;

    RestIndex( RestRequest restRequest, String indexName, RestGraphDatabase restGraphDatabase ) {
        this.restRequest = restRequest;
        this.indexName = indexName;
        this.restGraphDatabase = restGraphDatabase;
    }

    public String getName() {
        return indexName;
    }

    private String getTypeName() {
        return getEntityType().getSimpleName().toLowerCase();
    }

    public void add( T entity, String key, Object value ) {
        String uri = ( (RestEntity) entity ).getUri();
        restRequest.post( indexPath( key, value ), JsonHelper.createJsonFrom( uri ) );
    }

    protected String indexPath( ) {
        return "index/" + getTypeName() + "/" + indexName;
    }

    protected String indexPath( String key ) {
        return indexPath() + "/" + RestRequest.encode( key );
    }

    protected String indexPath( String key, Object value ) {
        return indexPath( key ) + "/" + RestRequest.encode( value );
    }

    public void remove( T entity, String key, Object value ) {
        restRequest.delete( entityIndexPath(indexPath( key, value ) , entity ) );
    }
    public void remove( T entity ) {
        restRequest.delete( entityIndexPath(indexPath( ) , entity ) );
    }

    public void remove(T entity, String key) {
        restRequest.delete(entityIndexPath(indexPath(key), entity));
    }

    private String entityIndexPath(String indexPath, T entity) {
        return indexPath + "/" + ( (RestEntity) entity ).getId();
    }

    public void delete() {
        restRequest.delete( indexPath( ));
    }

    public IndexHits<T> get( String key, Object value ) {
        return query( key, value );
    }

    public IndexHits<T> query( String key, Object value ) {
        ClientResponse response = restRequest.get( indexPath( key ) + "?query=" +  RestRequest.encode( value ) );
        if ( restRequest.statusIs( response, Response.Status.OK ) ) {
            Collection hits = (Collection) restRequest.toEntity( response );
            return new SimpleIndexHits<T>( hits, hits.size() );
        } else {
            return new SimpleIndexHits<T>( Collections.emptyList(), 0 );
        }
    }

    protected abstract T createEntity( Map<?, ?> item );

    public IndexHits<T> query( Object value ) {
        throw new UnsupportedOperationException();
    }

    private class SimpleIndexHits<T extends PropertyContainer> implements IndexHits<T> {
        private Collection<Object> hits;
        private int size;
        private Iterator<Object> iterator;

        public SimpleIndexHits( Collection<Object> hits, int size ) {
            this.hits = hits;
            this.iterator = this.hits.iterator();
            this.size = size;
        }

        public int size() {
            return size;
        }

        public void close() {

        }

        public T getSingle() {
            Iterator<Object> it = hits.iterator();
            return it.hasNext() ? transform( it.next() ) : null;
        }

        public float currentScore() {
            return 0;
        }

        public Iterator<T> iterator() {
            return this;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public T next() {
            Object value = iterator.next();
            return transform( value );
        }

        private T transform( Object value ) {
            return (T) createEntity( (Map<?, ?>) value );
        }

        public void remove() {

        }
    }
}
