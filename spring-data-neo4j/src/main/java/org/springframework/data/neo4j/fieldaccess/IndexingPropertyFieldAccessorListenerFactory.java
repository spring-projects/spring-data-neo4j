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

import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.lang.reflect.AnnotatedElement;


public class IndexingPropertyFieldAccessorListenerFactory<S extends PropertyContainer, T> implements FieldAccessorListenerFactory {

    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;
    private final IndexProvider indexProvider;
    private final Neo4jTemplate template;

    public IndexingPropertyFieldAccessorListenerFactory(final Neo4jTemplate template, final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
        this.template = template;
        indexProvider = new IndexProvider<S,T>(template);
    	this.propertyFieldAccessorFactory = propertyFieldAccessorFactory;
        this.convertingNodePropertyFieldAccessorFactory = convertingNodePropertyFieldAccessorFactory;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return isPropertyField(property) && property.isIndexed();
    }


    private boolean isPropertyField(final Neo4jPersistentProperty property) {
        return propertyFieldAccessorFactory.accept(property) || convertingNodePropertyFieldAccessorFactory.accept(property);
    }

    @Override
    public FieldAccessListener forField(Neo4jPersistentProperty property) {
        return new IndexingPropertyFieldAccessorListener(property, indexProvider, template);
    }


    public static class IndexProvider<S extends PropertyContainer, T> {
        private final Neo4jTemplate template;

        public IndexProvider(Neo4jTemplate template) {
            this.template = template;
        }

        private String getIndexKey(Neo4jPersistentProperty property) {
            Indexed indexed = property.getAnnotation(Indexed.class);
            if (indexed==null || indexed.fieldName().isEmpty()) return property.getNeo4jPropertyName();
            return indexed.fieldName();
        }

        private Indexed getIndexedAnnotation(AnnotatedElement element) {
            return element.getAnnotation(Indexed.class);
        }

        private Index<S> getIndex(Neo4jPersistentProperty property, Object instance) {
            final Indexed indexedAnnotation = property.getAnnotation(Indexed.class);
            @SuppressWarnings("unchecked") final Class<T> type = (Class<T>) property.getOwner().getType();
            final String providedIndexName = indexedAnnotation.indexName().isEmpty() ? null : indexedAnnotation.indexName();
            String indexName = Indexed.Name.get(indexedAnnotation.level(), type, providedIndexName, instance.getClass());
            if (!property.getIndexInfo().isFulltext()) {
                return template.getIndex(type, indexName, false);
            }
            if (providedIndexName == null) throw new IllegalStateException("@Indexed(fullext=true) on "+property+" requires an providedIndexName too ");
            String defaultIndexName = Indexed.Name.get(indexedAnnotation.level(), type, null, instance.getClass());
            if (providedIndexName.equals(defaultIndexName)) throw new IllegalStateException("Full-index name for "+property+" must differ from the default name: "+defaultIndexName);
            return template.getIndex(type, indexName, true);
        }
    }

    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingPropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener {

	    private final static Log log = LogFactory.getLog( IndexingPropertyFieldAccessorListener.class );

	    protected final String indexKey;
        private final Neo4jPersistentProperty property;
        private final IndexProvider indexProvider;
        private final Neo4jTemplate template;

        public IndexingPropertyFieldAccessorListener(final Neo4jPersistentProperty property, IndexProvider indexProvider, Neo4jTemplate template) {
            this.property = property;
            this.indexProvider = indexProvider;
            this.template = template;
            indexKey = indexProvider.getIndexKey(property);
        }

	    @Override
        public void valueChanged(Object entity, Object oldVal, Object newVal) {
            @SuppressWarnings("unchecked") Index<T> index = indexProvider.getIndex(property, entity);
            if (newVal instanceof Number) newVal = ValueContext.numeric((Number) newVal);

            final T state = template.getPersistentState(entity);
            index.remove(state, indexKey);
            if (newVal != null) {
                index.add(state, indexKey, newVal);
            }
        }
    }
}
