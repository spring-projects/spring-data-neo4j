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
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;


class IndexingNodePropertyFieldAccessorListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {

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
        final NodeEntity entityAnnotation = f.getDeclaringClass().getAnnotation(NodeEntity.class);
        if (entityAnnotation!=null && entityAnnotation.fullIndex()) return true;
	    final Indexed indexedAnnotation = f.getAnnotation(Indexed.class);
        if (indexedAnnotation != null) return true;
        final GraphProperty propertyAnnotation = f.getAnnotation(GraphProperty.class);
        return propertyAnnotation!=null && propertyAnnotation.index();
    }

    private boolean isPropertyField(final Field f) {
        return propertyFieldAccessorFactory.accept(f) || convertingNodePropertyFieldAccessorFactory.accept(f);
    }

    @Override
    public FieldAccessListener<NodeBacked,?> forField(final Field field) {
        return new IndexingNodePropertyFieldAccessorListener(field, graphDatabaseContext, getIndexName(field));
    }

    private String getIndexName(Field field) {
        Indexed indexed = field.getAnnotation(Indexed.class);
        return indexed != null ? indexed.name() : null;
    }

    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingNodePropertyFieldAccessorListener implements FieldAccessListener<NodeBacked, Object> {

	    private final static Log log = LogFactory.getLog( IndexingNodePropertyFieldAccessorListener.class );

	    protected final String indexKey;
	    private final GraphDatabaseContext graphDatabaseContext;
        private String indexName;

        public IndexingNodePropertyFieldAccessorListener(final Field field, final GraphDatabaseContext graphDatabaseContext, final String indexName) {
	        this.indexKey = DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
	        this.graphDatabaseContext = graphDatabaseContext;
            this.indexName = indexName;
        }

	    @Override
	    public void valueChanged(final NodeBacked nodeBacked, final Object oldVal, final Object newVal) {
            if (newVal==null) graphDatabaseContext.removeIndex(indexName, nodeBacked.getUnderlyingState(), indexKey);
	        else graphDatabaseContext.index(indexName, nodeBacked.getUnderlyingState(), indexKey, newVal.toString());
	    }

	}
}
