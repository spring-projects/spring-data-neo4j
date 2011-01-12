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

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.annotation.GraphTraversal;
import org.springframework.data.graph.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.doReturn;

public class TraversalFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {

	private FinderFactory finderFactory;

	
    public TraversalFieldAccessorFactory(FinderFactory finderFactory) {
		super();
		this.finderFactory = finderFactory;
	}


	@Override
    public boolean accept(final Field f) {
        final GraphTraversal graphEntityTraversal = f.getAnnotation(GraphTraversal.class);
        return graphEntityTraversal != null
                && graphEntityTraversal.traversalBuilder() != FieldTraversalDescriptionBuilder.class
                && f.getType().equals(Iterable.class);
    }


    @Override
    public FieldAccessor<NodeBacked> forField(final Field field) {
        return new TraversalFieldAccessor(field, finderFactory);
    }

	/**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class TraversalFieldAccessor implements FieldAccessor<NodeBacked> {
	    protected final Field field;
	    private final FinderFactory finderFactory;
	    private final FieldTraversalDescriptionBuilder fieldTraversalDescriptionBuilder;
	    private Class<? extends NodeBacked> target;

	    public TraversalFieldAccessor(final Field field, FinderFactory finderFactory) {
	        this.field = field;
	        this.finderFactory = finderFactory;
            final GraphTraversal graphEntityTraversal = field.getAnnotation(GraphTraversal.class);
	        this.target = graphEntityTraversal.elementClass();
	        this.fieldTraversalDescriptionBuilder = createTraversalDescription(graphEntityTraversal);
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
	        final NodeFinder<? extends NodeBacked> finder = finderFactory.createNodeEntityFinder(target);
	        final TraversalDescription traversalDescription = fieldTraversalDescriptionBuilder.build(nodeBacked,field);
	        return doReturn(finder.findAllByTraversal(nodeBacked, traversalDescription));
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

	}
}
