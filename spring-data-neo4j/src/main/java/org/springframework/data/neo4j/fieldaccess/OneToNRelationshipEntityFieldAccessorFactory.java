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

import org.neo4j.graphdb.*;
import org.springframework.dao.InvalidDataAccessApiUsageException;


import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class OneToNRelationshipEntityFieldAccessorFactory implements FieldAccessorFactory {

	private GraphDatabaseContext graphDatabaseContext;
	
	public OneToNRelationshipEntityFieldAccessorFactory(
			GraphDatabaseContext graphDatabaseContext) {
		super();
		this.graphDatabaseContext = graphDatabaseContext;
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
		return property.isRelationship() && !property.getRelationshipInfo().targetsNodes() && property.getRelationshipInfo().isMultiple();
	}

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
		return new OneToNRelationshipEntityFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<?>) relationshipInfo.getTargetType().getType(), graphDatabaseContext,property);
	}
	public static class OneToNRelationshipEntityFieldAccessor extends AbstractNodeRelationshipFieldAccessor<Node, Relationship> {

	    public OneToNRelationshipEntityFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> elementClass, final GraphDatabaseContext graphDatabaseContext, Neo4jPersistentProperty property) {
	        super(elementClass, graphDatabaseContext, direction, type, property);
	    }

	    @Override
	    public Object setValue(final Object entity, final Object newVal) {
	        throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	    }

	    @Override
	    public boolean isWriteable(Object entity) {
	        return false;
	    }

	    @Override
	    public Object getValue(final Object entity) {
	        checkUnderlyingNode(entity);
	        final Set<?> result = createEntitySetFromRelationships(entity);
	        return doReturn(new ManagedFieldAccessorSet(entity, result, property,graphDatabaseContext, this));
	    }

	    private Set<?> createEntitySetFromRelationships(final Object entity) {
	        final Set<Object> result = new HashSet<Object>();
	        for (final Relationship rel : getStatesFromEntity(entity)) {
	            final Object relationshipEntity = graphDatabaseContext.createEntityFromState(rel, relatedType);
	            result.add(relationshipEntity);
	        }
	        return result;
	    }

	    @Override
	    protected Iterable<Relationship> getStatesFromEntity(final Object entity) {
            final Node persistentState = property.getOwner().getPersistentState(entity, graphDatabaseContext);
            return persistentState.getRelationships(type, direction);
	    }

	    @Override
	    protected Relationship obtainSingleRelationship(final Node start, final Relationship end) {
	        return null;
	    }

	    @Override
	    protected Node getState(final Object entity) {
	        return property.getOwner().getPersistentState(entity, graphDatabaseContext);
	    }

	}
}
