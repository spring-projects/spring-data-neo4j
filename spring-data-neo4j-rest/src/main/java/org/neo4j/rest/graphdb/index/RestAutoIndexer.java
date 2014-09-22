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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.AutoIndexer;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.ReadableIndex;
import org.neo4j.rest.graphdb.RestAPI;

import java.util.Set;

public class RestAutoIndexer<T extends PropertyContainer> implements AutoIndexer<T> {

    protected final RestAPI restApi;
    protected final Class forClass;
    protected final ReadableIndex<T> autoIndex;


    public RestAutoIndexer(RestAPI restApi, Class forClass, ReadableIndex<T> autoIndex) {
        this.restApi = restApi;
        this.forClass = forClass;
        this.autoIndex = autoIndex;
    }

    @Override
    public void setEnabled(boolean b) {
        restApi.setAutoIndexingEnabled(forClass, b);
    }

    @Override
    public boolean isEnabled() {
        return restApi.isAutoIndexingEnabled(forClass);
    }

    @Override
    public ReadableIndex<T> getAutoIndex() {
        return autoIndex;
    }

    @Override
    public void startAutoIndexingProperty(String s) {
        restApi.startAutoIndexingProperty(forClass, s);
    }

    @Override
    public void stopAutoIndexingProperty(String s) {
        restApi.stopAutoIndexingProperty(forClass, s);
    }

    @Override
    public Set<String> getAutoIndexedProperties() {
        return restApi.getAutoIndexedProperties(forClass);
    }
}
