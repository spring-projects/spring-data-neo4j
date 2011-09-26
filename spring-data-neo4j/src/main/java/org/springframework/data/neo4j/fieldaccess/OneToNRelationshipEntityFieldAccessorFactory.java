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
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.core.RelationshipBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class OneToNRelationshipEntityFieldAccessorFactory implements FieldAccessorFactory<NodeBacked> {

	private GraphDatabaseContext graphDatabaseContext;
	
	public OneToNRelationshipEntityFieldAccessorFactory(
			GraphDatabaseContext graphDatabaseContext) {
		super();
		this.graphDatabaseContext = graphDatabaseContext;
	}

	@Override
	public boolean accept(final Neo4JPersistentProperty property) {
		return property.isRelationship() && !property.getRelationshipInfo().targetsNodes() && property.getRelationshipInfo().isMultiple();
	}

	@Override
	public FieldAccessor<NodeBacked> forField(final Neo4JPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
		return new OneToNRelationshipEntityFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<? extends RelationshipBacked>) relationshipInfo.getTargetType().getType(), graphDatabaseContext,property);
	}
	public static class OneToNRelationshipEntityFieldAccessor extends AbstractNodeRelationshipFieldAccessor<NodeBacked, Node, RelationshipBacked, Relationship> {

	    public OneToNRelationshipEntityFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends RelationshipBacked> elementClass, final GraphDatabaseContext graphDatabaseContext, Neo4JPersistentProperty property) {
	        super(elementClass, graphDatabaseContext, direction, type, property);
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
	        return doReturn(new ManagedFieldAccessorSet<NodeBacked, RelationshipBacked>(entity, result, property));
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
	        return entity.getPersistentState().getRelationships(type, direction);
	    }

	    @Override
	    protected Relationship obtainSingleRelationship(final Node start, final Relationship end) {
	        return null;
	    }

	    @Override
	    protected Node getState(final NodeBacked nodeBacked) {
	        return nodeBacked.getPersistentState();
	    }

	}
}
