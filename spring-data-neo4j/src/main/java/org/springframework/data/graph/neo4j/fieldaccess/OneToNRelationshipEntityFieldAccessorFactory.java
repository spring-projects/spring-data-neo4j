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

import org.neo4j.graphdb.*;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.annotation.RelatedToVia;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.doReturn;

public class OneToNRelationshipEntityFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {

	private GraphDatabaseContext graphDatabaseContext;
	
	public OneToNRelationshipEntityFieldAccessorFactory(
			GraphDatabaseContext graphDatabaseContext) {
		super();
		this.graphDatabaseContext = graphDatabaseContext;
	}

	@Override
	public boolean accept(final Field f) {
		return Iterable.class.isAssignableFrom(f.getType()) && hasValidRelationshipAnnotation(f);
	}

	@Override
	public FieldAccessor<NodeBacked> forField(final Field field) {
		final RelatedToVia relEntityAnnotation = getRelationshipAnnotation(field);
		return new OneToNRelationshipEntityFieldAccessor(typeFrom(relEntityAnnotation), dirFrom(relEntityAnnotation), targetFrom(relEntityAnnotation), graphDatabaseContext);
	}

	private boolean hasValidRelationshipAnnotation(final Field f) {
		final RelatedToVia relEntityAnnotation = getRelationshipAnnotation(f);
		return relEntityAnnotation != null && !RelationshipBacked.class.equals(relEntityAnnotation.elementClass());
	}

	private RelatedToVia getRelationshipAnnotation(final Field field) {
        return field.getAnnotation(RelatedToVia.class);
    }

	private Class<? extends RelationshipBacked> targetFrom(final RelatedToVia relEntityAnnotation) {
		return relEntityAnnotation.elementClass();
	}

	private Direction dirFrom(final RelatedToVia relEntityAnnotation) {
		return relEntityAnnotation.direction().toNeo4jDir();
	}

	private DynamicRelationshipType typeFrom(final RelatedToVia relEntityAnnotation) {
		return DynamicRelationshipType.withName(relEntityAnnotation.type());
	}

	public static class OneToNRelationshipEntityFieldAccessor extends AbstractNodeRelationshipFieldAccessor<NodeBacked, Node, RelationshipBacked, Relationship> {

	    public OneToNRelationshipEntityFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends RelationshipBacked> elementClass, final GraphDatabaseContext graphDatabaseContext) {
	        super(elementClass, graphDatabaseContext, direction, type);
	    }

	    @Override
	    public Object setValue(final NodeBacked entity, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	    }

	    @Override
	    public boolean isWriteable(NodeBacked nodeBacked) {
	        return false;
	    }

	    @Override
	    public Object getValue(final NodeBacked entity) {
	        checkUnderlyingNode(entity);
	        final Set<RelationshipBacked> result = createEntitySetFromRelationships(entity);
	        return doReturn(new ManagedFieldAccessorSet<NodeBacked, RelationshipBacked>(entity, result, this));
	    }

	    private Set<RelationshipBacked> createEntitySetFromRelationships(final NodeBacked entity) {
	        final Set<RelationshipBacked> result = new HashSet<RelationshipBacked>();
	        for (final Relationship rel : getStatesFromEntity(entity)) {
	            final RelationshipBacked relationshipEntity = graphDatabaseContext.createEntityFromState(rel, relatedType);
	            result.add(relationshipEntity);
	        }
	        return result;
	    }

	    @Override
	    protected Iterable<Relationship> getStatesFromEntity(final NodeBacked entity) {
	        return entity.getUnderlyingState().getRelationships(type, direction);
	    }

	    @Override
	    protected Relationship obtainSingleRelationship(final Node start, final Relationship end) {
	        return null;
	    }

	    @Override
	    protected Node getState(final NodeBacked nodeBacked) {
	        return nodeBacked.getUnderlyingState();
	    }

	}
}
