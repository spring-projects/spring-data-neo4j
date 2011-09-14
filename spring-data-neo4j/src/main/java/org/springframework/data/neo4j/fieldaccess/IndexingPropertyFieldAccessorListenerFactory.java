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

package org.springframework.data.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.index.Index;
import org.neo4j.index.lucene.ValueContext;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;


public class IndexingPropertyFieldAccessorListenerFactory<S extends PropertyContainer, T extends GraphBacked<S>> implements FieldAccessorListenerFactory<T> {

    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;
    private final IndexInfo indexInfo;

    public IndexingPropertyFieldAccessorListenerFactory(final GraphDatabaseContext graphDatabaseContext, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        indexInfo = new IndexInfo<S,T>(graphDatabaseContext);
    	this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Field f) {
        return isPropertyField(f) && indexInfo.isIndexed(f);
    }


    private boolean isPropertyField(final Field f) {
        return propertyFieldAccessorFactory.accept(f) || convertingNodePropertyFieldAccessorFactory.accept(f);
    }

    @Override
    public FieldAccessListener<T, ?> forField(Field field) {
        return (FieldAccessListener<T, ?>) new IndexingPropertyFieldAccessorListener(field,indexInfo);
    }


    public static class IndexInfo<S extends PropertyContainer, T extends GraphBacked<S>> {
        private final GraphDatabaseContext graphDatabaseContext;

        public IndexInfo(GraphDatabaseContext graphDatabaseContext) {
            this.graphDatabaseContext = graphDatabaseContext;
        }

        private boolean isIndexed(final Field f) {
            final Indexed indexedAnnotation = getIndexedAnnotation(f);
            return indexedAnnotation != null;
        }
        private String getIndexKey(Field field) {
            Indexed indexed = getIndexedAnnotation(field);
            if (indexed==null || indexed.fieldName().isEmpty()) return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
            return indexed.fieldName();
        }

        private boolean isFulltextIndex(Field field) {
            Indexed indexed = getIndexedAnnotation(field);
            return indexed!=null && indexed.fulltext();
        }

        private Indexed getIndexedAnnotation(AnnotatedElement element) {
            return element.getAnnotation(Indexed.class);
        }

        private Index<S> getIndex(Field field, T instance) {
            final Indexed indexedAnnotation = getIndexedAnnotation(field);
            final Class<T> type = (Class<T>) field.getDeclaringClass();
            final String providedIndexName = indexedAnnotation.indexName().isEmpty() ? null : indexedAnnotation.indexName();
            String indexName = Indexed.Name.get(indexedAnnotation.level(), type, providedIndexName, instance.getClass());
            if (!isFulltextIndex(field)) {
                return graphDatabaseContext.getIndex(type, indexName, false);
            }
            if (providedIndexName == null) throw new IllegalStateException("@Indexed(fullext=true) on "+field+" requires an providedIndexName too ");
            String defaultIndexName = Indexed.Name.get(indexedAnnotation.level(), type, null, instance.getClass());
            if (providedIndexName.equals(defaultIndexName)) throw new IllegalStateException("Full-index name for "+field+" must differ from the default name: "+defaultIndexName);
            return graphDatabaseContext.getIndex(type, indexName, true);
        }
    }

    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingPropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener<GraphBacked<T>, Object> {

	    private final static Log log = LogFactory.getLog( IndexingPropertyFieldAccessorListener.class );

	    protected final String indexKey;
        private final Field field;
        private final IndexInfo indexInfo;
        private Index<T> index;

        public IndexingPropertyFieldAccessorListener(final Field field, IndexInfo indexInfo) {
            this.field = field;
            this.indexInfo = indexInfo;
            indexKey = indexInfo.getIndexKey(field);
        }

	    @Override
        public void valueChanged(GraphBacked<T> graphBacked, Object oldVal, Object newVal) {
            if (index==null) {
                index = indexInfo.getIndex(field, graphBacked);
            }
            if (newVal instanceof Number) newVal = ValueContext.numeric((Number) newVal);

            final T state = graphBacked.getPersistentState();
            //index.remove(state, indexKey);
            if (newVal == null) {
                index.remove(state, indexKey);
            } else {
                index.add(state, indexKey, newVal);
            }
        }
    }
}
