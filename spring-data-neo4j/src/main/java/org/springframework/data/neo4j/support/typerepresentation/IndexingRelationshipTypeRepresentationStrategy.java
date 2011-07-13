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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.persistence.EntityInstantiator;

import java.util.HashMap;
import java.util.Map;

public class IndexingRelationshipTypeRepresentationStrategy implements RelationshipTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;
    private GraphDatabaseService graphDb;
    private final Map<String,Class<?>> cache=new HashMap<String, Class<?>>();

    public IndexingRelationshipTypeRepresentationStrategy(GraphDatabaseService graphDb,
                                                          EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
		this.graphDb = graphDb;
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

	private Index<Node> getNodeTypesIndex() {
		return graphDb.index().forNodes(INDEX_NAME);
	}

	private Index<Relationship> getRelTypesIndex() {
		return graphDb.index().forRelationships(INDEX_NAME);
	}

	@Override
	public void postEntityCreation(Relationship state, Class<? extends RelationshipBacked> type) {
        addToTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
	}

    private void addToTypesIndex(Relationship node, Class<? extends RelationshipBacked> entityClass) {
		Class<?> klass = entityClass;
		while (klass.getAnnotation(RelationshipEntity.class) != null) {
			getRelTypesIndex().add(node, INDEX_KEY, klass.getName());
			klass = klass.getSuperclass();
		}
	}

    @Override
    public <U extends RelationshipBacked> ClosableIterable<U> findAll(Class<U> clazz) {
        return findAllRelBacked(clazz);
    }

    private <ENTITY extends RelationshipBacked> ClosableIterable<ENTITY> findAllRelBacked(Class<ENTITY> clazz) {
        final IndexHits<Relationship> allEntitiesOfType = getRelTypesIndex().get(INDEX_KEY, clazz.getName());
        return new FilteringClosableEntityIterable<ENTITY>(allEntitiesOfType);

    }


    @Override
    public long count(Class<? extends RelationshipBacked> entityClass) {
        long count = 0;
        for (Object o : getRelTypesIndex().get(INDEX_KEY, entityClass.getName())) {
            count += 1;
        }
		return count;
	}

    @Override
    @SuppressWarnings("unchecked")
    public Class<? extends RelationshipBacked> getJavaType(Relationship relationship) {
		if (relationship == null) throw new IllegalArgumentException("Node is null");
        String className = (String) relationship.getProperty(TYPE_PROPERTY_NAME);
        return getClassForName(className);
    }

    @SuppressWarnings({"unchecked"})
    private <ENTITY extends GraphBacked<?>> Class<ENTITY> getClassForName(String className) {
        try {
            Class<ENTITY> result= (Class<ENTITY>) cache.get(className);
            if (result!=null) return result;
            synchronized (cache) {
                result= (Class<ENTITY>) cache.get(className);
                if (result!=null) return result;
                result = (Class<ENTITY>) Class.forName(className);
                cache.put(className,result);
                return result;
            }
		} catch (NotFoundException e) {
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
    }


    @Override
    public void preEntityRemoval(Relationship state) {
        getRelTypesIndex().remove(state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends RelationshipBacked> U createEntity(Relationship state) {
        Class<? extends RelationshipBacked> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on relationship.");
        }
        return (U) relationshipEntityInstantiator.createEntityFromState(state, javaType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends RelationshipBacked> U createEntity(Relationship state, Class<U> type) {
        Class<? extends RelationshipBacked> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on relationship.");
        }
        if (type.isAssignableFrom(javaType)) {
            return (U) relationshipEntityInstantiator.createEntityFromState(state, javaType);
        }
        throw new IllegalArgumentException(String.format("Entity is not of type: %s (was %s)", type, javaType));
    }

    @Override
    public <U extends RelationshipBacked> U projectEntity(Relationship state, Class<U> type) {
        return relationshipEntityInstantiator.createEntityFromState(state, type);
    }

    private class FilteringClosableEntityIterable<ENTITY extends RelationshipBacked> extends FilteringIterable<ENTITY> implements ClosableIterable<ENTITY> {
        private final IndexHits<Relationship> indexHits;

        public FilteringClosableEntityIterable(IndexHits<Relationship> indexHits) {
            super(new IterableWrapper<ENTITY, Relationship>(indexHits) {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected ENTITY underlyingObjectToObject(Relationship rel) {
                            Class<ENTITY> javaType = (Class<ENTITY>) IndexingRelationshipTypeRepresentationStrategy.this.getJavaType(rel);
                            if (javaType == null) return null;
                            return IndexingRelationshipTypeRepresentationStrategy.this.relationshipEntityInstantiator.createEntityFromState(rel, javaType);
                        }
                    }, new Predicate<ENTITY>() {
                        @Override
                        public boolean accept(ENTITY item) {
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
