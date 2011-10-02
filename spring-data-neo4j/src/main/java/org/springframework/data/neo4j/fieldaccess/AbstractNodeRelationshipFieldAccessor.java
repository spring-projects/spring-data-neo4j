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
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public abstract class AbstractNodeRelationshipFieldAccessor<STATE extends PropertyContainer,TSTATE extends PropertyContainer> implements FieldAccessor {
    protected final RelationshipType type;
    protected final Neo4jPersistentProperty property;
    protected final Direction direction;
    protected final Class<?> relatedType;
    protected final GraphDatabaseContext graphDatabaseContext;

    public AbstractNodeRelationshipFieldAccessor(Class<?> clazz, GraphDatabaseContext graphDatabaseContext, Direction direction, RelationshipType type, Neo4jPersistentProperty property) {
        this.relatedType = clazz;
        this.graphDatabaseContext = graphDatabaseContext;
        this.direction = direction;
        this.type = type;
        this.property = property;
    }

    @Override
    public boolean isWriteable(Object entity) {
        return true;
    }

    protected STATE checkUnderlyingNode(Object entity) {
        if (entity==null) throw new IllegalStateException("Entity is null");
        STATE node = getState(entity);
        if (node != null) return node;
        throw new IllegalStateException("Entity must have a backing Node");
    }

    protected void removeMissingRelationships(Node node, Set<Node> targetNodes) {
        for ( Relationship relationship : node.getRelationships(type, direction) ) {
            if (!targetNodes.remove(relationship.getOtherNode(node)))
                relationship.delete();
        }
    }

    protected void createAddedRelationships(STATE node, Set<TSTATE> targetNodes) {
        for (TSTATE targetNode : targetNodes) {
            createSingleRelationship(node,targetNode);
        }
    }
    // adding cascade
    protected Set<STATE> checkTargetIsSetOfNodebacked(Object newVal) {
        if (!(newVal instanceof Set)) {
            throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
        }
        Set<STATE> nodes=new HashSet<STATE>();
        for (Object value : (Set<Object>) newVal) {
            if (!relatedType.isInstance(value)) {
                throw new IllegalArgumentException("New value elements must be "+relatedType);
            }
//            nodes.add(getState(value));
              nodes.add(getOrCreateState(value));
        }
        return nodes;
    }

    protected STATE getOrCreateState(Object value) {
        final STATE state = getState(value);
        if (state != null) return state;
        final Object saved = graphDatabaseContext.save(value);
        final STATE newState = getState(saved);
        Assert.notNull(newState);
        return newState;
    }

    protected <T> ManagedFieldAccessorSet<T> createManagedSet(Object entity, Set<T> result) {
        return new ManagedFieldAccessorSet<T>(entity, result, property,graphDatabaseContext,this);
    }

    protected Set<Object> createEntitySetFromRelationshipEndNodes(Object entity) {
        final Iterable<TSTATE> nodes = getStatesFromEntity(entity);
        final Set<Object> result = new HashSet<Object>();
        for (final TSTATE otherNode : nodes) {
            Object target=graphDatabaseContext.createEntityFromState(otherNode, relatedType);
            result.add(target);
		}
        return result;
    }


    protected void createSingleRelationship(STATE start, TSTATE end) {
        if (end==null) return;
        switch(direction) {
            case OUTGOING :
            case BOTH : { // TODO both should actually check in both directions, perhaps have the obtain method get the direction instead and figure out what to do itself
                obtainSingleRelationship(start, end);
                break;
            }
            case INCOMING :
                obtainSingleRelationship((STATE)end, (TSTATE)start);
                break;
            default : throw new InvalidDataAccessApiUsageException("invalid direction " + direction);
        }
    }

	public Object getDefaultImplementation() {
        return null;
	}
	
    protected abstract Relationship obtainSingleRelationship(STATE start, TSTATE end);

    protected abstract Iterable<TSTATE> getStatesFromEntity(Object entity);

    protected abstract STATE
    getState(Object entity);
}
