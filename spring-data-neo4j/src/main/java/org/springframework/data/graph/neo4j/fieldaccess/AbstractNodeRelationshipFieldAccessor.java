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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.*;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public abstract class AbstractNodeRelationshipFieldAccessor<ENTITY extends GraphBacked,STATE extends PropertyContainer,TARGET extends GraphBacked,TSTATE extends PropertyContainer> implements FieldAccessor<ENTITY> {
    protected final RelationshipType type;
    protected final Direction direction;
    protected final Class<? extends TARGET> relatedType;
    protected final GraphDatabaseContext graphDatabaseContext;

    public AbstractNodeRelationshipFieldAccessor(Class<? extends TARGET> clazz, GraphDatabaseContext graphDatabaseContext, Direction direction, RelationshipType type) {
        this.relatedType = clazz;
        this.graphDatabaseContext = graphDatabaseContext;
        this.direction = direction;
        this.type = type;
    }

    @Override
    public boolean isWriteable(ENTITY entity) {
        return true;
    }

    protected STATE checkUnderlyingNode(ENTITY entity) {
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

    protected void checkNoCircularReference(Node node, Set<STATE> targetNodes) {
        if (targetNodes.contains(node)) throw new InvalidDataAccessApiUsageException("Cannot create a circular reference to "+ targetNodes);
    }

    protected Set<STATE> checkTargetIsSetOfNodebacked(Object newVal) {
        if (!(newVal instanceof Set)) {
            throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
        }
        Set<STATE> nodes=new HashSet<STATE>();
        for (Object value : (Set<Object>) newVal) {
            if (!relatedType.isInstance(value)) {
                throw new IllegalArgumentException("New value elements must be "+relatedType);
            }
            nodes.add(getState((ENTITY)value));
        }
        return nodes;
    }

    protected ManagedFieldAccessorSet<ENTITY,TARGET> createManagedSet(ENTITY entity, Set<TARGET> result) {
        return new ManagedFieldAccessorSet<ENTITY,TARGET>(entity, result, this);
    }

    protected Set<TARGET> createEntitySetFromRelationshipEndNodes(ENTITY entity) {
        final Iterable<TSTATE> nodes = getStatesFromEntity(entity);
        final Set<TARGET> result = new HashSet<TARGET>();
        for (final TSTATE otherNode : nodes) {
            TARGET target=graphDatabaseContext.createEntityFromState(otherNode, relatedType);
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

    protected abstract Relationship obtainSingleRelationship(STATE start, TSTATE end);

    protected abstract Iterable<TSTATE> getStatesFromEntity(ENTITY entity);

    protected abstract STATE getState(ENTITY entity);
}
