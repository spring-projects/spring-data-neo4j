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

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.GraphTraversal;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.support.GenericTypeExtractor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class TraversalFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {
	@Override
    public boolean accept(final Field f) {
        final GraphTraversal graphEntityTraversal = f.getAnnotation(GraphTraversal.class);
        return graphEntityTraversal != null
                && graphEntityTraversal.traversalBuilder() != FieldTraversalDescriptionBuilder.class
                && f.getType().equals(Iterable.class);
    }


    @Override
    public FieldAccessor<NodeBacked> forField(final Field field) {
        return new TraversalFieldAccessor(field);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class TraversalFieldAccessor implements FieldAccessor<NodeBacked> {
	    protected final Field field;
	    private final FieldTraversalDescriptionBuilder fieldTraversalDescriptionBuilder;
	    private Class<? extends NodeBacked> target;
        protected String[] params;

        public TraversalFieldAccessor(final Field field) {
	        this.field = field;
            final GraphTraversal graphEntityTraversal = field.getAnnotation(GraphTraversal.class);
	        this.target = resolveTarget(graphEntityTraversal,field);
            this.params = graphEntityTraversal.params();
            this.fieldTraversalDescriptionBuilder = createTraversalDescription(graphEntityTraversal);
	    }

        private Class<? extends NodeBacked> resolveTarget(GraphTraversal graphTraversal, Field field) {
            if (!graphTraversal.elementClass().equals(Object.class)) return graphTraversal.elementClass();
            final Class<?> result = GenericTypeExtractor.resolveFieldType(field);
            if (!NodeBacked.class.isAssignableFrom(result)) throw new IllegalArgumentException("The target result type of the traversal is no node entity: "+field);
            return (Class<? extends NodeBacked>) result;
        }


	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object setValue(final NodeBacked nodeBacked, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set readonly traversal description field " + field);
	    }

	    @Override
	    public Object getValue(final NodeBacked nodeBacked) {
	        final TraversalDescription traversalDescription = fieldTraversalDescriptionBuilder.build(nodeBacked,field,params);
	        return doReturn(nodeBacked.findAllByTraversal((Class<? extends NodeBacked>) target, traversalDescription));
	    }


	    private FieldTraversalDescriptionBuilder createTraversalDescription(final GraphTraversal graphEntityTraversal) {
	        try {
	            final Class<? extends FieldTraversalDescriptionBuilder> traversalDescriptionClass = graphEntityTraversal.traversalBuilder();
	            final Constructor<? extends FieldTraversalDescriptionBuilder> constructor = traversalDescriptionClass.getDeclaredConstructor();
	            constructor.setAccessible(true);
	            return constructor.newInstance();
	        } catch (Exception e) {
	            throw new RuntimeException("Error creating TraversalDescription from " + field,e);
	        }
	    }

		@Override
		public Object getDefaultImplementation() {
			return null;
		}

	}
}
