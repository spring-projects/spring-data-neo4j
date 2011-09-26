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
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;


public class IndexingPropertyFieldAccessorListenerFactory<S extends PropertyContainer, T extends GraphBacked<S>> implements FieldAccessorListenerFactory<T> {

    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;
    private final IndexProvider indexProvider;

    public IndexingPropertyFieldAccessorListenerFactory(final GraphDatabaseContext graphDatabaseContext, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        indexProvider = new IndexProvider<S,T>(graphDatabaseContext);
    	this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Neo4JPersistentProperty property) {
        return isPropertyField(property) && property.isIndexed();
    }


    private boolean isPropertyField(final Neo4JPersistentProperty property) {
        return propertyFieldAccessorFactory.accept(property) || convertingNodePropertyFieldAccessorFactory.accept(property);
    }

    @Override
    public FieldAccessListener<T, ?> forField(Neo4JPersistentProperty property) {
        return (FieldAccessListener<T, ?>) new IndexingPropertyFieldAccessorListener(property, indexProvider);
    }


    public static class IndexProvider<S extends PropertyContainer, T extends GraphBacked<S>> {
        private final GraphDatabaseContext graphDatabaseContext;

        public IndexProvider(GraphDatabaseContext graphDatabaseContext) {
            this.graphDatabaseContext = graphDatabaseContext;
        }

        private String getIndexKey(Neo4JPersistentProperty property) {
            Indexed indexed = property.getAnnotation(Indexed.class);
            if (indexed==null || indexed.fieldName().isEmpty()) return property.getNeo4jPropertyName();
            return indexed.fieldName();
        }

        private Indexed getIndexedAnnotation(AnnotatedElement element) {
            return element.getAnnotation(Indexed.class);
        }

        private Index<S> getIndex(Neo4JPersistentProperty property, GraphBacked instance) {
            final Indexed indexedAnnotation = property.getAnnotation(Indexed.class);
            final Class<T> type = (Class<T>) property.getOwner().getType();
            final String providedIndexName = indexedAnnotation.indexName().isEmpty() ? null : indexedAnnotation.indexName();
            String indexName = Indexed.Name.get(indexedAnnotation.level(), type, providedIndexName, instance.getClass());
            if (!property.getIndexInfo().isFulltext()) {
                return graphDatabaseContext.getIndex(type, indexName, false);
            }
            if (providedIndexName == null) throw new IllegalStateException("@Indexed(fullext=true) on "+property+" requires an providedIndexName too ");
            String defaultIndexName = Indexed.Name.get(indexedAnnotation.level(), type, null, instance.getClass());
            if (providedIndexName.equals(defaultIndexName)) throw new IllegalStateException("Full-index name for "+property+" must differ from the default name: "+defaultIndexName);
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
        private final Neo4JPersistentProperty property;
        private final IndexProvider indexProvider;

        public IndexingPropertyFieldAccessorListener(final Neo4JPersistentProperty property, IndexProvider indexProvider) {
            this.property = property;
            this.indexProvider = indexProvider;
            indexKey = indexProvider.getIndexKey(property);
        }

	    @Override
        public void valueChanged(GraphBacked<T> graphBacked, Object oldVal, Object newVal) {
            Index<T> index = indexProvider.getIndex(property, graphBacked);
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
