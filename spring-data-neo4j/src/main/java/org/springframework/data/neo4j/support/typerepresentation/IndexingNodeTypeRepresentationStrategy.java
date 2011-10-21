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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.mapping.EntityInstantiator;

public class IndexingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private EntityInstantiator<Node> graphEntityInstantiator;
    private GraphDatabaseService graphDb;
    private final EntityTypeCache typeCache;

    public IndexingNodeTypeRepresentationStrategy(GraphDatabaseService graphDb,
                                                  EntityInstantiator<Node> graphEntityInstantiator) {
		this.graphDb = graphDb;
		this.graphEntityInstantiator = graphEntityInstantiator;
        typeCache = new EntityTypeCache();
    }

	private Index<Node> getNodeTypesIndex() {
		return graphDb.index().forNodes(INDEX_NAME);
	}

	private Index<Relationship> getRelTypesIndex() {
		return graphDb.index().forRelationships(INDEX_NAME);
	}

	@Override
	public void postEntityCreation(Node state, Class<?> type) {
        addToNodeTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
	}

    private void addToNodeTypesIndex(Node node, Class<?> entityClass) {
		Class<?> klass = entityClass;
		while (klass.getAnnotation(NodeEntity.class) != null) {
			getNodeTypesIndex().add(node, INDEX_KEY, klass.getName());
			klass = klass.getSuperclass();
		}
	}

    @Override
    public <U> ClosableIterable<U> findAll(Class<U> clazz) {
        return findAllNodeEntity(clazz);
    }

    private <Object> ClosableIterable<Object> findAllNodeEntity(Class<Object> clazz) {
		final IndexHits<Node> allEntitiesOfType = getNodeTypesIndex().get(INDEX_KEY, clazz.getName());
        return new FilteringClosableIterable<Object>(allEntitiesOfType);
	}

    @Override
    public long count(Class<?> entityClass) {
        long count = 0;
        for (Object o : getNodeTypesIndex().get(INDEX_KEY, entityClass.getName())) {
            count += 1;
        }
		return count;
	}

    @SuppressWarnings("unchecked")
    @Override
    public Class<?> getJavaType(Node node) {
		if (node == null) throw new IllegalArgumentException("Node is null");
        String className = (String) node.getProperty(TYPE_PROPERTY_NAME);
        return typeCache.getClassForName(className);
    }

    @Override
	public void preEntityRemoval(Node state) {
        getNodeTypesIndex().remove(state);
	}

    @Override
    @SuppressWarnings("unchecked")
    public <U> U createEntity(Node state) {
        Class<?> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on node.");
        }
        return (U) graphEntityInstantiator.createEntityFromState(state, javaType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> U createEntity(Node state, Class<U> type) {
        Class<?> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on node.");
        }
        if (type.isAssignableFrom(javaType)) {
            return (U) graphEntityInstantiator.createEntityFromState(state, javaType);
        }
        throw new IllegalArgumentException(String.format("Entity is not of type: %s (was %s)", type, javaType));
    }

    @Override
    public <U> U projectEntity(Node state, Class<U> type) {
        return graphEntityInstantiator.createEntityFromState(state, type);
    }

    private class FilteringClosableIterable<Object> extends FilteringIterable<Object> implements ClosableIterable<Object> {
        private final IndexHits<Node> indexHits;

        public FilteringClosableIterable(IndexHits<Node> indexHits) {
            super(new IterableWrapper<Object, Node>(indexHits) {
                        @Override
                        @SuppressWarnings("unchecked")
                        protected Object underlyingObjectToObject(Node node) {
                            Class<Object> javaType = (Class<Object>) getJavaType(node);
                            if (javaType == null) return null;
                            return graphEntityInstantiator.createEntityFromState(node, javaType);
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
            indexHits.close();
        }
    }
}
