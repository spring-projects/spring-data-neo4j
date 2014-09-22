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

import java.util.Collection;
import java.util.Iterator;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.index.IndexHits;

import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.UpdatableRestResult;
import org.neo4j.rest.graphdb.converter.RestEntityExtractor;

/**
 * @author mh
 * @since 22.09.11
 */
public class SimpleIndexHits<T extends PropertyContainer> implements IndexHits<T>, UpdatableRestResult<SimpleIndexHits<T>> {
    private Collection<Object> hits;
    private Class<T> entityType;
    private int size;
    private Iterator<Object> iterator;
    private RestEntityExtractor entityExtractor;

    public SimpleIndexHits(long batchId, Class<T> entityType, final RestAPI restApi) {
        this.entityType = entityType;
        this.entityExtractor = restApi.getEntityExtractor();

    }

    public SimpleIndexHits(Collection<Object> hits, int size, Class<T> entityType, final RestAPI restApi) {
        this.hits = hits;
        this.entityType = entityType;
        this.iterator = this.hits.iterator();
        this.size = size;
        this.entityExtractor = restApi.getEntityExtractor();
    }

    public int size() {
        return size;
    }

    public void close() {

    }

    public T getSingle() {
        Iterator<Object> it = hits.iterator();
        return it.hasNext() ? transform(it.next()) : null;
    }

    public float currentScore() {
        return 0;
    }

    public ResourceIterator<T> iterator() {
        return this;
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public T next() {
        Object value = iterator.next();
        return transform(value);
    }

    private T transform(Object value) {
        return (T) entityExtractor.convertFromRepresentation(value);
    }

    public void remove() {

    }

    @Override
    public void updateFrom(SimpleIndexHits<T> newValue, RestAPI restApi) {
        this.hits= newValue.hits;
        this.iterator = this.hits.iterator();
        this.size = newValue.size;
    }
}
