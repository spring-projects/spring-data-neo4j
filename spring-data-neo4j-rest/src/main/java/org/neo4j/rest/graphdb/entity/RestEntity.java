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
import java.util.LinkedHashMap;
import java.util.Map;

import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.rest.graphdb.*;
import org.neo4j.rest.graphdb.util.ArrayConverter;
import org.springframework.data.neo4j.core.UpdateableState;

import static org.neo4j.helpers.collection.MapUtil.map;

public abstract class RestEntity implements PropertyContainer, UpdatableRestResult<RestEntity>, UpdateableState {
    private Map<?, ?> structuralData;
    protected Map<String, Object> propertyData;
    private long lastTimeFetchedPropertyData;
    protected RestAPI restApi;
    private Long id;

    private final ArrayConverter arrayConverter=new ArrayConverter();
    private String uri;

    public RestEntity( URI uri, RestAPI restApi ) {
        this( uri.toString(), restApi );
    }    

    public RestEntity( String uri, RestAPI restApi ) {
        this.uri = uri;
        this.id = getEntityId(uri);
        this.restApi = restApi;
    }      

    public RestEntity( Map<?, ?> data, RestAPI restApi ) {
        this.restApi = restApi;
        this.structuralData = data;
        this.uri = (String) data.get( "self" );
        this.id = getEntityId(uri);
        setProperties((Map<String, Object>) data.get("data"));
    }

    public RestEntity(long id, Map<String, Object> restData, RestAPI facade) {
        this.restApi = facade;
        this.structuralData = restData;
        this.uri = nodeUri(facade,id);
        this.id = id;
        setProperties((Map<String, Object>) structuralData.get("data"));
    }

    public static String nodeUri(RestAPI facade, long id) {
        return facade.getBaseUri()+"/node/" + id;
    }

    public String getUri() {       
        return uri;
    }
    
    public void updateFrom(RestEntity updateEntity, RestAPI restApi){
//        if (this == updateEntity){
//            this.lastTimeFetchedPropertyData = 0;
//        }
        this.uri = updateEntity.getUri();
        this.id = getEntityId(uri);
        this.structuralData = updateEntity.getStructuralData();
        if (updateEntity.lastTimeFetchedPropertyData > 0 && updateEntity.propertyData != null) {
            setProperties(updateEntity.propertyData);
        }
    }    

    Map<?, ?> getStructuralData() {
//        if ( this.structuralData == null ) {
//            this.structuralData = restApi.getData(this);
//        }
        return this.structuralData;
    }    
   
    Map<String, Object> getPropertyData() {       
        if (hasToUpdateProperties()) {
            doUpdate();
        }
        return this.propertyData;
    }

    protected abstract void doUpdate();

    protected void setProperties(Map<String, Object> properties) {
        tracking = false;
        this.propertyData = properties;
        this.lastTimeFetchedPropertyData = System.currentTimeMillis();
    }

    protected boolean hasToUpdateProperties() {
        if (tracking) return false;
        if (this.propertyData == null) return true;
        return restApi.hasToUpdate(this.lastTimeFetchedPropertyData);
    }


    public Object getProperty( String key ) {
        Object value = getPropertyValue(key);
        if ( value == null ) {
            throw new NotFoundException( "'" + key + "' on " + this );
        }
        return value;
    }

    private Object getPropertyValue( String key ) {
        Map<String, Object> properties = getPropertyData();
        Object value = properties.get( key );
        if ( value == null) return null;
        if ( value instanceof Collection ) {
            Collection col= (Collection) value;
            if (col.isEmpty()) return new String[0]; // todo concrete value type ?
            Object result = arrayConverter.toArray( col );
            if (result == null) throw new IllegalStateException( "Could not determine type of property "+key );
            properties.put(key,result);
            return result;

        }
        return PropertiesMap.assertSupportedPropertyValue( value );
    }

    public Object getProperty( String key, Object defaultValue ) {
        Object value = getPropertyValue( key );
        return value != null ? value : defaultValue;
    }

    @SuppressWarnings("unchecked")
    public Iterable<String> getPropertyKeys() {
        return new IterableWrapper( getPropertyData().keySet() ) {
            @Override
            protected String underlyingObjectToObject( Object key ) {
                return key.toString();
            }
        };
    }

    @SuppressWarnings("unchecked")
    public Iterable<Object> getPropertyValues() {
        return (Iterable<Object>) getPropertyData().values();
    }

    public boolean hasProperty( String key ) {
        return getPropertyData().containsKey( key );
    }

    public Object removeProperty( String key ) {
        Object value = getProperty( key, null );
        if (!tracking) restApi.removeProperty(this, key);
        if (this.propertyData != null ) this.propertyData.remove(key);
        return value;
    }

    public void setProperty( String key, Object value ) {
        if (!tracking) this.restApi.setPropertyOnEntity(this, key, value);
        if (this.propertyData == null) this.propertyData = new LinkedHashMap<>();
        this.propertyData.put(key,value);
    }

    private boolean tracking;

    @Override
    public void flush() {
        if (!tracking) return;
        if (propertyData != null && !propertyData.isEmpty()) {
            this.restApi.setPropertiesOnEntity(this,propertyData);
        }
        tracking = false;
    }

    @Override
    public void track() {
        this.tracking = true;
    }

    public static long getEntityId( String uri ) {
        if (uri.startsWith("{")) return -1;
        return Long.parseLong(uri.substring(uri.lastIndexOf('/') + 1));
    }

    public long getId() {        
        return id;
    }

    public void delete() {
        this.restApi.deleteEntity(this);
    }

    @Override
    public int hashCode() {
        return (int) getId();
    }

    @Override
    public boolean equals( Object o ) {
        if (o == null) return false;
        return getClass().equals( o.getClass() ) && getId() == ( (RestEntity) o ).getId();
    }

       
    public RestGraphDatabase getGraphDatabase() {
    	 return new RestGraphDatabase(restApi);
    }

    @Override
    public String toString() {
        return getUri();
    }
    
    public RestAPI getRestApi() {
		return restApi;
	}

    public void setLastTimeFetchedPropertyData(long lastTimeFetchedPropertyData) {
        this.lastTimeFetchedPropertyData = lastTimeFetchedPropertyData;
    }

    @Override
    public void refresh() {
        doUpdate();
    }

    @Override
    public void addPropertiesBatch(Map<String, Object> properties) {
        setProperties(properties);
        restApi.setPropertiesOnEntity(this, propertyData);
    }

    @Override
    public void addAllLabelsBatch(Collection<String> labels) { }
}
