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

import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.api.NodeBacked;
import org.springframework.data.annotation.Indexed;

import java.lang.reflect.Field;


@Configurable
class IndexingNodePropertyFieldAccessorListenerFactory implements FieldAccessorListenerFactory<NodeBacked> {
    @Autowired
    private IndexService indexService;
    private final PropertyFieldAccessorFactory propertyFieldAccessorFactory;
    private final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory;

    IndexingNodePropertyFieldAccessorListenerFactory(final PropertyFieldAccessorFactory propertyFieldAccessorFactory, final ConvertingNodePropertyFieldAccessorFactory convertingNodePropertyFieldAccessorFactory) {
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
        return new IndexingNodePropertyFieldAccessorListener(field,indexService);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class IndexingNodePropertyFieldAccessorListener implements FieldAccessListener<NodeBacked, Object> {
	    protected final Field field;
	    private final IndexService indexService;

	    public IndexingNodePropertyFieldAccessorListener(final Field field, final IndexService indexService) {
	        this.field = field;
	        this.indexService = indexService;
	    }

	    @Override
	    public void valueChanged(final NodeBacked nodeBacked, final Object oldVal, final Object newVal) {
	        if (newVal==null) indexService.removeIndex(nodeBacked.getUnderlyingState(),getPropertyName());
	        else indexService.index(nodeBacked.getUnderlyingState(), getPropertyName(),newVal);
	    }

	    // todo
	    private String getPropertyName() {
	        return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
	    }

	}
}
