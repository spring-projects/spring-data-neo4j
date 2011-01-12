/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;


class IndexingNodePropertyFieldAccessorListenerFactory<T extends GraphBacked<?>> implements FieldAccessorListenerFactory<T> {

    private final GraphDatabaseContext graphDatabaseContext;
    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;

    IndexingNodePropertyFieldAccessorListenerFactory(final GraphDatabaseContext graphDatabaseContext, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
    	this.graphDatabaseContext = graphDatabaseContext;
    	this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Field f) {
        return isPropertyField(f) && isIndexed(f);
    }

    private boolean isIndexed(final Field f) {
        final Indexed indexedAnnotation = getIndexedAnnotation(f);
        if (indexedAnnotation != null) return true;
        final NodeEntity entityAnnotation = f.getDeclaringClass().getAnnotation(NodeEntity.class);
        return (entityAnnotation!=null && entityAnnotation.fullIndex());
    }

    private boolean isPropertyField(final Field f) {
        return propertyFieldAccessorFactory.accept(f) || convertingNodePropertyFieldAccessorFactory.accept(f);
    }

    @Override
    public FieldAccessListener<T, ?> forField(Field field) {
        String indexName = getIndexName(field);
        if (NodeBacked.class.isAssignableFrom(field.getDeclaringClass())) {
            return (FieldAccessListener<T, ?>) new IndexingNodePropertyFieldAccessorListener<Node>(field, graphDatabaseContext.getNodeIndex(indexName));
        }
        return (FieldAccessListener<T, ?>) new IndexingNodePropertyFieldAccessorListener<Relationship>(field, graphDatabaseContext.getRelationshipIndex(indexName));
    }

    private String getIndexName(Field field) {
        Indexed indexed = getIndexedAnnotation(field);
        if (hasIndexName(indexed)) return indexed.indexName();

        final Indexed indexedEntity = getIndexedAnnotation(field.getDeclaringClass());
        return hasIndexName( indexedEntity ) ?  indexedEntity.indexName() : null;
    }

    private Indexed getIndexedAnnotation(AnnotatedElement element) {
        return element.getAnnotation(Indexed.class);
    }

    private boolean hasIndexName(Indexed indexed) {
        return indexed!=null && !indexed.indexName().isEmpty();
    }

    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingNodePropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener<GraphBacked<T>, Object> {

	    private final static Log log = LogFactory.getLog( IndexingNodePropertyFieldAccessorListener.class );

	    protected final String indexKey;
        private final Index<T> index;

        public IndexingNodePropertyFieldAccessorListener(final Field field,  final Index<T> index) {
	        this.indexKey = DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
            this.index = index;
        }

	    @Override
        public void valueChanged(GraphBacked<T> graphBacked, Object oldVal, Object newVal) {
            if (indexKey.contains("years"))
            System.out.println("index listener call on "+graphBacked+" "+graphBacked.getUnderlyingState()+"field = " + indexKey+" newVal "+newVal);

            if (newVal==null) index.remove(graphBacked.getUnderlyingState(), indexKey, null);
	        else index.add(graphBacked.getUnderlyingState(), indexKey, newVal);
	    }
    }
}
