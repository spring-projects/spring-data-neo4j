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

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.ClosableIndexHits;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexType;

public class IndexingRelationshipTypeRepresentationStrategy implements RelationshipTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__rel_types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private final GraphDatabase graphDb;
    private final EntityTypeCache typeCache;
    
    private final IndexProvider indexProvider;

    public IndexingRelationshipTypeRepresentationStrategy(GraphDatabase graphDb, IndexProvider indexProvider) {
		this.graphDb = graphDb;
		this.indexProvider = indexProvider;
        typeCache = new EntityTypeCache();
    }

	private Index<Relationship> getRelTypesIndex() {
		return graphDb.createIndex(Relationship.class,INDEX_NAME, IndexType.SIMPLE);
	}

	@Override
	public void postEntityCreation(Relationship state, Class<?> type) {
        addToTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
	}

    private void addToTypesIndex(Relationship node, Class<?> entityClass) {
        Class<?> type = entityClass;
        while (type.getAnnotation(RelationshipEntity.class) != null) {
            String value = entityClass.getName();
            if (indexProvider != null)
                value = indexProvider.createIndexValueForType(entityClass);

            getRelTypesIndex().add(node, INDEX_KEY, value);
            type = type.getSuperclass();
        }
    }

    @Override
    public <U> ClosableIterable<Relationship> findAll(Class<U> clazz) {
        return findAllRelBacked(clazz);
    }

    private <Object> ClosableIterable<Relationship> findAllRelBacked(Class<Object> clazz) {
        String value = clazz.getName();
        if (indexProvider != null)
            value = indexProvider.createIndexValueForType(clazz);
        
        final IndexHits<Relationship> allEntitiesOfType = getRelTypesIndex().get(INDEX_KEY, value);
        return new ClosableIndexHits<Relationship>(allEntitiesOfType);
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

}
