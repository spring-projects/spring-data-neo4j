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
package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntityImpl;

import static org.springframework.data.neo4j.support.ParameterCheck.notNull;

/**
 * @author mh
 * @since 17.10.11
 */
class IndexProvider {
    private Neo4jMappingContext mappingContext;
    private final GraphDatabase graphDatabase;

    IndexProvider(Neo4jMappingContext mappingContext, GraphDatabase graphDatabase) {
        this.mappingContext = mappingContext;
        this.graphDatabase = graphDatabase;
    }

    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type) {
        return getIndex(type, null);
    }

    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName) {
        return getIndex(type, indexName, null);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName, Boolean fullText) {
        if (type == null) {
            notNull(indexName, "indexName");
            return getIndex(indexName);
        }

        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        if (indexName == null) indexName = Indexed.Name.get(type);
        final boolean useExistingIndex = fullText == null;

        if (useExistingIndex) {
            if (persistentEntity.isNodeEntity()) return (Index<S>) graphDatabase.getIndex(indexName);
            if (persistentEntity.isRelationshipEntity()) return (Index<S>) graphDatabase.getIndex(indexName);
            throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
        }

        if (persistentEntity.isNodeEntity()) return (Index<S>) createIndex(Node.class, indexName, fullText);
        if (persistentEntity.isRelationshipEntity())
            return (Index<S>) createIndex(Relationship.class, indexName, fullText);
        throw new IllegalArgumentException("Wrong index type supplied: " + type + " expected Node- or Relationship-Entity");
    }

    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> Index<T> getIndex(String indexName) {
        return graphDatabase.getIndex(indexName);
    }

    public boolean isNode(Class<? extends PropertyContainer> type) {
        if (type.equals(Node.class)) return true;
        if (type.equals(Relationship.class)) return false;
        throw new IllegalArgumentException("Unknown Graph Primitive, neither Node nor Relationship" + type);
    }

    // TODO handle existing indexes
    @SuppressWarnings("unchecked")
    public <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText) {
        return graphDatabase.createIndex(type, indexName, fullText);
    }
}
