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

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.annotation.RelationshipEntity;


import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.mapping.EntityInstantiator;

public class IndexingRelationshipTypeRepresentationStrategy implements RelationshipTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__rel_types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private EntityInstantiator<Relationship> relationshipEntityInstantiator;
    private GraphDatabaseService graphDb;
    private final EntityTypeCache typeCache;

    public IndexingRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDb,
                                                          EntityInstantiator<Relationship> relationshipEntityInstantiator) {
		this.graphDb = graphDb;
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
        typeCache = new EntityTypeCache();
    }

	private Index<Relationship> getRelTypesIndex() {
		return graphDb.index().forRelationships(INDEX_NAME);
	}

	@Override
	public void postEntityCreation(Relationship state, Class<?> type) {
        addToTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
	}

    private void addToTypesIndex(Relationship node, Class<?> entityClass) {
		Class<?> type = entityClass;
		while (type.getAnnotation(RelationshipEntity.class) != null) {
			getRelTypesIndex().add(node, INDEX_KEY, type.getName());
			type = type.getSuperclass();
		}
	}

    @Override
    public <U> ClosableIterable<U> findAll(Class<U> clazz) {
        return findAllRelBacked(clazz);
    }

    private <Object> ClosableIterable<Object> findAllRelBacked(Class<Object> clazz) {
        final IndexHits<Relationship> allEntitiesOfType = getRelTypesIndex().get(INDEX_KEY, clazz.getName());
        return new FilteringClosableEntityIterable<Object>(allEntitiesOfType);

    }


    @Override
    public long count(Class<?> entityClass) {
        long count = 0;
        final IndexHits<Relationship> hits = getRelTypesIndex().get(INDEX_KEY, entityClass.getName());
        while (hits.hasNext()) {
            hits.next();
            count++;
        }
		return count;
	}

    @Override
    @SuppressWarnings("unchecked")
    public Class<?> getJavaType(Relationship relationship) {
		if (relationship == null) throw new IllegalArgumentException("Relationship is null");
        String className = (String) relationship.getProperty(TYPE_PROPERTY_NAME);
        return typeCache.getClassForName(className);
    }


    @Override
    public void preEntityRemoval(Relationship state) {
        getRelTypesIndex().remove(state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U createEntity(Relationship state) {
        Class<?> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on relationship.");
        }
        return (U) relationshipEntityInstantiator.createEntityFromState(state, javaType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U createEntity(Relationship state, Class<U> type) {
        Class<?> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on relationship.");
        }
        if (type.isAssignableFrom(javaType)) {
            return (U) relationshipEntityInstantiator.createEntityFromState(state, javaType);
        }
        throw new IllegalArgumentException(String.format("Entity is not of type: %s (was %s)", type, javaType));
    }

    @Override
    public <U> U projectEntity(Relationship state, Class<U> type) {
        return relationshipEntityInstantiator.createEntityFromState(state, type);
    }

    private class FilteringClosableEntityIterable<Object> extends FilteringIterable<Object> implements ClosableIterable<Object> {
        private final IndexHits<Relationship> indexHits;

        public FilteringClosableEntityIterable(IndexHits<Relationship> indexHits) {
            super(new IterableWrapper<Object, Relationship>(indexHits) {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected Object underlyingObjectToObject(Relationship rel) {
                            Class<Object> javaType = (Class<Object>) IndexingRelationshipTypeRepresentationStrategy.this.getJavaType(rel);
                            if (javaType == null) return null;
                            return IndexingRelationshipTypeRepresentationStrategy.this.relationshipEntityInstantiator.createEntityFromState(rel, javaType);
                        }
                    }, new Predicate<Object>() {
                        @Override
                        public boolean accept(Object item) {
                            return item != null;
                        }
                    });
            this.indexHits = indexHits;
        }

        @Override
        public void close() {
           this.indexHits.close();
        }
    }
}
