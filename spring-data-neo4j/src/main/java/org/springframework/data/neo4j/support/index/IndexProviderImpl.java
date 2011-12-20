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

import static org.springframework.data.neo4j.support.ParameterCheck.*;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.Neo4jPersistentEntityImpl;

/**
 * @author mh
 * @since 17.10.11
 */
public class IndexProviderImpl implements IndexProvider {
    private final Neo4jMappingContext mappingContext;
    private final GraphDatabase graphDatabase;

    public IndexProviderImpl(Neo4jMappingContext mappingContext, GraphDatabase graphDatabase) {
        this.mappingContext = mappingContext;
        this.graphDatabase = graphDatabase;
    }

    @Override
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        return getIndex(type, null);
    }

    @Override
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
        return getIndex(type, indexName, null);
    }
    @Override
    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, IndexType indexType) {
        if (type == null) {
            notNull(indexName, "indexName");
            return getIndex(indexName);
        }

        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        if (indexName == null) indexName = customizeIndexName(Indexed.Name.get(type), type);
        final boolean useExistingIndex = indexType == null;

        if (useExistingIndex) {
            if (persistentEntity.isNodeEntity() || persistentEntity.isRelationshipEntity()) return (Index<S>) graphDatabase.getIndex(indexName);
            throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
        }

        if (persistentEntity.isNodeEntity())
            return (Index<S>) createIndex(Node.class, indexName, indexType);
        if (persistentEntity.isRelationshipEntity())
            return (Index<S>) createIndex(Relationship.class, indexName, indexType);
        throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        return graphDatabase.getIndex(indexName);
    }

    @Override
    public boolean isNode(Class<? extends PropertyContainer> type) {
        if (type.equals(Node.class)) return true;
        if (type.equals(Relationship.class)) return false;
        throw new IllegalArgumentException("Unknown Graph Primitive, neither Node nor Relationship" + type);
    }

    // TODO handle existing indexes
    @Override
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, IndexType fullText) {
        return graphDatabase.createIndex(type, indexName, fullText);
    }

    @Override
    public <S extends PropertyContainer> Index<S> getIndex(Neo4jPersistentProperty property, final Class<?> instanceType) {
        final Indexed indexedAnnotation = property.getAnnotation(Indexed.class);
        final Class<?> declaringType = property.getOwner().getType();
        final String providedIndexName = indexedAnnotation==null || indexedAnnotation.indexName().isEmpty() ? null : indexedAnnotation.indexName();
        final Indexed.Level level = indexedAnnotation == null ? Indexed.Level.CLASS : indexedAnnotation.level();
        String indexName = customizeIndexName(Indexed.Name.get(level, declaringType, providedIndexName, instanceType), instanceType);
        if (!property.isIndexed() || property.getIndexInfo().getIndexType() == IndexType.SIMPLE) {
            return getIndex(declaringType, indexName, IndexType.SIMPLE);
        }
        String defaultIndexName = customizeIndexName(Indexed.Name.get(level, declaringType, null, instanceType.getClass()), instanceType);
        if (providedIndexName==null || providedIndexName.equals(defaultIndexName)) throw new IllegalStateException("Index name for "+property+" must differ from the default name: "+defaultIndexName);
        return getIndex(declaringType, indexName, property.getIndexInfo().getIndexType());
    }
    
    @Override
    public String createIndexValueForType(Class<?> type) {
        return type.getName();
    }
    
    @Override
    public String customizeIndexName(String indexName, Class<?> type) {
        return indexName;
    }
 
}
