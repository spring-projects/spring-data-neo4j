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
package org.neo4j.rest.graphdb.index;


import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.index.lucene.QueryContext;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 * @author mh
 * @since 24.01.11
 */
public abstract class RestIndex<T extends PropertyContainer> implements Index<T> {
    private final String indexName;
    public String getIndexName() {
        return indexName;
    }

    protected final RestAPI restApi;

    RestIndex(String indexName, RestAPI restApi) {
        this.indexName = indexName;
        this.restApi = restApi;
    }

    @Override
    public GraphDatabaseService getGraphDatabase() {
       return new RestGraphDatabase(restApi);
    }

    private String getTypeName() {
        return getEntityType().getSimpleName().toLowerCase();
    }

    public void add( T entity, String key, Object value ) {
       restApi.addToIndex(entity, this, key, value);
    }
    public T putIfAbsent( T entity, String key, Object value ) {
       return restApi.putIfAbsent(entity, this, key, value);
    }


    public void remove( T entity, String key, Object value ) {
       restApi.removeFromIndex(this, entity, key, value);
    }  

    public void remove(T entity, String key) {
       restApi.removeFromIndex(this, entity, key);
    }

    public void remove(T entity) {       
        restApi.removeFromIndex(this, entity);
    }

    public void delete() {
       restApi.delete(this);
    }

    public org.neo4j.graphdb.index.IndexHits<T> get( String key, Object value ) {
        return restApi.getIndex(getEntityType(), indexName, key, value);
    }


    public IndexHits<T> query( String key, Object value ) {
        return restApi.queryIndex(getEntityType(), indexName, key, value);
    }

    public org.neo4j.graphdb.index.IndexHits<T> query( Object value ) {
        if (value instanceof QueryContext) {
            value = ((QueryContext)value).getQueryOrQueryObject();
        }
        return query("null",value);
    }
    
    public String getName() {
        return indexName;
    }
/*
    public RestRequest getRestRequest() {
        return restRequest;
    }
    private Long getBatchId(Map<String, Object> entry) {
        return ((Number) entry.get("id")).longValue();
    }
*/
}
