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
package org.springframework.data.neo4j.support.index;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.ReadableIndex;

/**
 * @author mh
 * @since 16.10.11
 */
public class NullReadableIndex<S extends PropertyContainer> implements ReadableIndex<S> {
    private final String indexName;
    private final GraphDatabaseService graphDatabaseService;

    public NullReadableIndex(String indexName, GraphDatabaseService graphDatabaseService) {
        this.indexName = indexName;
        this.graphDatabaseService = graphDatabaseService;
    }

    @Override
    public String getName() {
        return indexName;
    }

    @Override
    public GraphDatabaseService getGraphDatabase() {
        return graphDatabaseService;
    }

    @Override
    public Class<S> getEntityType() {
        return null;
    }

    @Override
    public IndexHits<S> get(String key, Object value) {
        return new EmptyIndexHits<S>();
    }

    @Override
    public IndexHits<S> query(String key, Object queryOrQueryObject) {
        return new EmptyIndexHits<S>();
    }

    @Override
    public IndexHits<S> query(Object queryOrQueryObject) {
        return new EmptyIndexHits<S>();
    }

    @Override
    public boolean isWriteable() {
        return false;
    }

}
