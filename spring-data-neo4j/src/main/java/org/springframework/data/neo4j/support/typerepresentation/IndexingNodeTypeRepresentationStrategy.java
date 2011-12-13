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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.NodeTypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.ClosableIndexHits;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexType;

public class IndexingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private final GraphDatabase graphDb;
    private final EntityTypeCache typeCache;
    
    private final IndexProvider indexProvider;

    public IndexingNodeTypeRepresentationStrategy(GraphDatabase graphDb, IndexProvider indexProvider) {
		this.graphDb = graphDb;
		this.indexProvider = indexProvider;
        typeCache = new EntityTypeCache();
    }

    private Index<Node> getNodeTypesIndex() {
        return graphDb.createIndex(Node.class, INDEX_NAME, IndexType.SIMPLE);
    }

    @Override
    public void postEntityCreation(Node state, Class<?> type) {
        addToNodeTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
    }

    private void addToNodeTypesIndex(Node node, Class<?> entityClass) {
        Class<?> klass = entityClass;

        while (klass.getAnnotation(NodeEntity.class) != null) {
            String value = klass.getName();
            if (indexProvider != null)
                value = indexProvider.createIndexValueForType(klass);

            getNodeTypesIndex().add(node, INDEX_KEY, value);
            klass = klass.getSuperclass();
        }
    }

    @Override
    public <U> ClosableIterable<Node> findAll(Class<U> clazz) {
        return findAllNodeBacked(clazz);
    }

    private <Object> ClosableIterable<Node> findAllNodeBacked(Class<Object> clazz) {
        String value = clazz.getName();
        if (indexProvider != null)
            value = indexProvider.createIndexValueForType(clazz);

        final IndexHits<Node> allEntitiesOfType = getNodeTypesIndex().get(INDEX_KEY, value);
        return new ClosableIndexHits<Node>(allEntitiesOfType);
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

}
