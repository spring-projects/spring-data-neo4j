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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.annotation.GraphTraversal;
import org.springframework.data.neo4j.core.FieldTraversalDescriptionBuilder;


import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Constructor;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class TraversalFieldAccessorFactory implements FieldAccessorFactory {
    private final GraphDatabaseContext graphDatabaseContext;

    public TraversalFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty f) {
        final GraphTraversal graphEntityTraversal = f.getAnnotation(GraphTraversal.class);
        return graphEntityTraversal != null
                && graphEntityTraversal.traversal() != FieldTraversalDescriptionBuilder.class
                && f.getType().equals(Iterable.class);
    }


    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new TraversalFieldAccessor(property,graphDatabaseContext);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class TraversalFieldAccessor implements FieldAccessor {
	    protected final Neo4jPersistentProperty property;
        private final GraphDatabaseContext graphDatabaseContext;
        private final FieldTraversalDescriptionBuilder fieldTraversalDescriptionBuilder;
	    private Class<?> target;
        protected String[] params;

        public TraversalFieldAccessor(final Neo4jPersistentProperty property, GraphDatabaseContext graphDatabaseContext) {
	        this.property = property;
            this.graphDatabaseContext = graphDatabaseContext;
            final GraphTraversal graphEntityTraversal = property.getAnnotation(GraphTraversal.class);
	        this.target = resolveTarget(graphEntityTraversal,property);
            this.params = graphEntityTraversal.params();
            this.fieldTraversalDescriptionBuilder = createTraversalDescription(graphEntityTraversal);
	    }

        private Class<?> resolveTarget(GraphTraversal graphTraversal, Neo4jPersistentProperty property) {
            if (!graphTraversal.elementClass().equals(Object.class)) return graphTraversal.elementClass();
            final Class<?> result = property.getTypeInformation().getActualType().getType();
            if (graphDatabaseContext.isNodeEntity(result)) return result;
            if (graphDatabaseContext.isRelationshipEntity(result)) return result;
            Class<?>[] allowedTypes={Node.class,Relationship.class, Path.class};
            if (!checkTypes(result,allowedTypes)) throw new IllegalArgumentException("The target result type "+result+" of the traversal is no subclass of the allowed types: "+property+" "+allowedTypes);
            return result;
        }

        private boolean checkTypes(Class<?> target, Class<?>...allowedTypes) {
            for (Class<?> type : allowedTypes) {
                if (type.isAssignableFrom(target)) return true;
            }
            return false;
        }


        @Override
	    public boolean isWriteable(Object entity) {
	        return false;
	    }

	    @Override
	    public Object setValue(final Object entity, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set readonly traversal description field " + property);
	    }

	    @Override
	    public Object getValue(final Object entity) {
	        final TraversalDescription traversalDescription = fieldTraversalDescriptionBuilder.build(entity, property,params);
            return doReturn(graphDatabaseContext.findAllByTraversal(entity,target, traversalDescription));
	    }


	    private FieldTraversalDescriptionBuilder createTraversalDescription(final GraphTraversal graphEntityTraversal) {
	        try {
	            final Class<? extends FieldTraversalDescriptionBuilder> traversalDescriptionClass = graphEntityTraversal.traversal();
	            final Constructor<? extends FieldTraversalDescriptionBuilder> constructor = traversalDescriptionClass.getDeclaredConstructor();
	            constructor.setAccessible(true);
	            return constructor.newInstance();
	        } catch (Exception e) {
	            throw new RuntimeException("Error creating TraversalDescription from " + property,e);
	        }
	    }

		@Override
		public Object getDefaultImplementation() {
			return null;
		}

	}
}
