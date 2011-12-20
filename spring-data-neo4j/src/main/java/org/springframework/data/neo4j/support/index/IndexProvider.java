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

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

public interface IndexProvider {

    <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type);

    <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName);

    @SuppressWarnings("unchecked")
    <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName,
            IndexType indexType);

    @SuppressWarnings("unchecked")
    <T extends PropertyContainer> Index<T> getIndex(String indexName);

    boolean isNode(Class<? extends PropertyContainer> type);

    // TODO handle existing indexes
    @SuppressWarnings("unchecked")
    <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName,
            IndexType fullText);

    <S extends PropertyContainer> Index<S> getIndex(Neo4jPersistentProperty property,
            final Class<?> instanceType);
    /**
     * adjust your indexName for the "__types__" indices
     * 
     * @param type
     * @return prefixed indexName for Type
     */
    String createIndexValueForType(Class<?> type);
    
    /**
     * possibility to do something with the high level index name 
     * 
     * @param indexName
     * @param type
     * @return
     */
    String customizeIndexName(String indexName, Class<?> type);

}