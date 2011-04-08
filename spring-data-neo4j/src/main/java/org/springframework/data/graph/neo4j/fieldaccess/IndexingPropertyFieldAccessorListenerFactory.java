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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.lucene.ValueContext;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.annotation.Indexed;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;


public class IndexingPropertyFieldAccessorListenerFactory<S extends PropertyContainer, T extends GraphBacked<S>> implements FieldAccessorListenerFactory<T> {

    private final GraphDatabaseContext graphDatabaseContext;
    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;

    public IndexingPropertyFieldAccessorListenerFactory(final GraphDatabaseContext graphDatabaseContext, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
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
        return indexedAnnotation != null;
    }

    private boolean isPropertyField(final Field f) {
        return propertyFieldAccessorFactory.accept(f) || convertingNodePropertyFieldAccessorFactory.accept(f);
    }

    @Override
    public FieldAccessListener<T, ?> forField(Field field) {
        Class<T> graphBacked = (Class<T>) field.getDeclaringClass();
        Index<? extends PropertyContainer> index = getIndex(field,graphBacked);
        String indexKey = getIndexKey(field);
        return (FieldAccessListener<T, ?>) new IndexingPropertyFieldAccessorListener(field, index, indexKey);
    }

    private String getIndexKey(Field field) {
        Indexed indexed = getIndexedAnnotation(field);
        if (indexed==null || indexed.fieldName().isEmpty()) return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
        return indexed.fieldName();
    }

    private Index<S> getIndex(Field field, Class<T> type) {
        if (!isFulltextIndex(field)) {
            return graphDatabaseContext.getIndex(type, Indexed.Name.get(field), false);
        }
        Indexed indexed = getIndexedAnnotation(field);
        if (indexed.indexName()==null) throw new IllegalStateException("@Indexed(fullext=true) on "+field+" requires an indexName too ");
        String defaultIndexName = Indexed.Name.getDefault(field);
        if (indexed.indexName().equals(defaultIndexName)) throw new IllegalStateException("Full-index name for "+field+" must differ from the default name: "+defaultIndexName);
        return graphDatabaseContext.getIndex(type, indexed.indexName(), true);
    }

    private boolean isFulltextIndex(Field field) {
        Indexed indexed = getIndexedAnnotation(field);
        return indexed!=null && indexed.fulltext();
    }

    private Indexed getIndexedAnnotation(AnnotatedElement element) {
        return element.getAnnotation(Indexed.class);
    }

    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingPropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener<GraphBacked<T>, Object> {

	    private final static Log log = LogFactory.getLog( IndexingPropertyFieldAccessorListener.class );

	    protected final String indexKey;
        private final Index<T> index;

        public IndexingPropertyFieldAccessorListener(final Field field, final Index<T> index, final String indexKey) {
            this.index = index;
            this.indexKey = indexKey;
        }

	    @Override
        public void valueChanged(GraphBacked<T> graphBacked, Object oldVal, Object newVal) {
            if (newVal instanceof Number) newVal = ValueContext.numeric((Number) newVal);

            if (newVal==null) index.remove(graphBacked.getPersistentState(), indexKey, null);
	        else index.add(graphBacked.getPersistentState(), indexKey, newVal);
	    }
    }
}
