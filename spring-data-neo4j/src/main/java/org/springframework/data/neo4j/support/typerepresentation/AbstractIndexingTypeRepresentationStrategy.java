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
package org.springframework.data.neo4j.support.typerepresentation;

import java.lang.annotation.Annotation;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.ClosableIndexHits;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexType;

public abstract class AbstractIndexingTypeRepresentationStrategy<S extends PropertyContainer> implements
        TypeRepresentationStrategy<S> {

    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    protected String INDEX_NAME;
    protected final GraphDatabase graphDb;
    protected final EntityTypeCache typeCache;
    protected final IndexProvider indexProvider;
    private final Class<? extends PropertyContainer> clazz;
    private final Class<? extends Annotation> typeEntityClass;

    public AbstractIndexingTypeRepresentationStrategy(GraphDatabase graphDb, IndexProvider indexProvider,
            final String indexName, final Class<? extends PropertyContainer> clazz,
            final Class<? extends Annotation> typeEntityClass) {
        this.graphDb = graphDb;
        this.indexProvider = indexProvider;
        typeCache = new EntityTypeCache();
        INDEX_NAME = indexName;
        this.clazz = clazz;
        this.typeEntityClass = typeEntityClass;
    }

    @SuppressWarnings("unchecked")
    public Index<S> getTypesIndex() {
        return (Index<S>) graphDb.createIndex(clazz, INDEX_NAME, IndexType.SIMPLE);
    }

    @Override
    public void postEntityCreation(S state, Class<?> type) {
        addToTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
    }

    @Override
    public long count(Class<?> entityClass) {
        long count = 0;
        final IndexHits<S> hits = getTypesIndex().get(INDEX_KEY, entityClass.getName());
        while (hits.hasNext()) {
            hits.next();
            count++;
        }
        return count;
    }

    @Override
    public void preEntityRemoval(S state) {
        getTypesIndex().remove(state);
    }

    @Override
    public <U> ClosableIterable<S> findAll(Class<U> clazz) {
        return findAllRelBacked(clazz);
    }

    @Override
    public <U> Class<U> getJavaType(S propertyContainer) {
        if (propertyContainer == null)
            throw new IllegalArgumentException("Relationship or Node is null");
        String className = (String) propertyContainer.getProperty(TYPE_PROPERTY_NAME);
        return typeCache.getClassForName(className);
    }

    protected void addToTypesIndex(S relationshipOrNode, Class<?> entityClass) {
        Class<?> type = entityClass;
        while (type.getAnnotation(typeEntityClass) != null) {
            String value = entityClass.getName();
            if (indexProvider != null)
                value = indexProvider.createIndexValueForType(entityClass);

            getTypesIndex().add(relationshipOrNode, INDEX_KEY, value);
            type = type.getSuperclass();
        }
    }

    @SuppressWarnings("hiding")
    private <Object> ClosableIterable<S> findAllRelBacked(Class<Object> clazz) {
        String value = clazz.getName();
        if (indexProvider != null)
            value = indexProvider.createIndexValueForType(clazz);

        final IndexHits<S> allEntitiesOfType = getTypesIndex().get(INDEX_KEY, value);
        return new ClosableIndexHits<S>(allEntitiesOfType);
    }

}
