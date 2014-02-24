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
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

@Deprecated
public interface IndexProvider {

    @Deprecated <S extends PropertyContainer, T> Index<S> getIndex(Neo4jPersistentEntity<T> type);

    @Deprecated <S extends PropertyContainer, T> Index<S> getIndex(Neo4jPersistentEntity<T> type, String indexName);

    @SuppressWarnings("unchecked")
    @Deprecated <S extends PropertyContainer, T> Index<S> getIndex(Neo4jPersistentEntity<T> persistentEntity, String indexName, IndexType indexType);

    @Deprecated <T extends PropertyContainer> Index<T> getIndex(String indexName);

    boolean isNode(Class<? extends PropertyContainer> type);

    // TODO handle existing indexes
    @SuppressWarnings("unchecked")
    @Deprecated <T extends PropertyContainer> Index<T> createIndex(Class<T> propertyContainerType, String indexName,
            IndexType fullText);

    @Deprecated <S extends PropertyContainer> Index<S> getIndex(Neo4jPersistentProperty property, final Class<?> instanceType);
    /**
     * adjust your indexName for the "__types__" indices
     * 
     * @return prefixed indexName for Type
     */
    @Deprecated String createIndexValueForType(Object type);
    
    /**
     * possibility to do something with the high level index name 
     */
    @Deprecated String customizeIndexName(String indexName, Class<?> type);

}
