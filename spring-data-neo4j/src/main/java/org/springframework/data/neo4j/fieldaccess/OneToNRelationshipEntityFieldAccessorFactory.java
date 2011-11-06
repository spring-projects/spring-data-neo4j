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

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.mapping.RelationshipProperties;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class OneToNRelationshipEntityFieldAccessorFactory implements FieldAccessorFactory {

	private Neo4jTemplate template;
	
	public OneToNRelationshipEntityFieldAccessorFactory(
			Neo4jTemplate template) {
		super();
		this.template = template;
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
		return property.isRelationship() && !property.getRelationshipInfo().targetsNodes() && property.getRelationshipInfo().isMultiple();
	}

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
		return new OneToNRelationshipEntityFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<?>) relationshipInfo.getTargetType().getType(), template,property);
	}
	public static class OneToNRelationshipEntityFieldAccessor extends AbstractNodeRelationshipFieldAccessor<Node, Relationship> {

        private final boolean isEditableSet;

        public OneToNRelationshipEntityFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> elementClass, final Neo4jTemplate template, Neo4jPersistentProperty property) {
	        super(elementClass, template, direction, type, property);
            isEditableSet = Set.class.isAssignableFrom(this.property.getType());
        }

	    @Override
	    public Object setValue(final Object entity, final Object newVal) {
	        if (!isEditableSet) throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
            final Node startNode = checkUnderlyingState(entity);
            if (newVal == null) {
   	            return null;
   	        }
            final Map<Node, Object> targetNodes = createSetOfTargetNodes(newVal, startNode);
   	        removeMissingRelationships(startNode, targetNodes.keySet());
   	        //createAddedRelationships(startNode, targetNodes.keySet());
            persistEntities(targetNodes);
            return createManagedSet(entity, (Set<?>) newVal);
	    }

        private void persistEntities(Map<Node, Object> targetNodes) {
            for (Object entry : targetNodes.values()) {
                template.save(entry);
            }
        }

        protected Map<Node, Object> createSetOfTargetNodes(Object newVal, Node startNode) {
            if (!(newVal instanceof Set)) {
                throw new IllegalArgumentException("New value must be a Set, was: " + newVal.getClass());
            }
            Map<Node,Object> targetNodes=new HashMap<Node,Object>();
            for (Object entry : (Set<Object>) newVal) {
                if (!relatedType.isInstance(entry)) {
                    throw new IllegalArgumentException("New value elements must be "+relatedType);
                }
                Neo4jPersistentEntity relationshipPEntity = property.getRelationshipInfo().getTargetEntity();
                final RelationshipProperties relationshipProperties = relationshipPEntity.getRelationshipProperties();
                final Node endNode = getState(relationshipProperties.getEndeNodeProperty().getValue(entry));
                if (!endNode.equals(startNode)) {
                    targetNodes.put(endNode, entry);
                } else {
                    final Node otherNode = getState(relationshipProperties.getStartNodeProperty().getValue(entry));
                    targetNodes.put(otherNode, entry);
                }
            }
            return targetNodes;
        }

	    @Override
	    public boolean isWriteable(Object entity) {
	        return isEditableSet;
	    }

	    @Override
	    public Object getValue(final Object entity) {
	        checkUnderlyingState(entity);
            final GraphBackedEntityIterableWrapper<Relationship, ?> result = iterableFrom(entity);
            if (isEditableSet) {
                @SuppressWarnings("unchecked") final ManagedFieldAccessorSet managedSet = createManagedSet(entity, IteratorUtil.addToCollection(result, new HashSet()));
                return doReturn(managedSet);
            }
            return doReturn(result);
        }

        private GraphBackedEntityIterableWrapper<Relationship, ?> iterableFrom(final Object entity) {
            return GraphBackedEntityIterableWrapper.create(getStatesFromEntity(entity), relatedType, template);
        }

	    @Override
	    protected Iterable<Relationship> getStatesFromEntity(final Object entity) {
            final Node node = getState(entity);
            return node.getRelationships(type, direction);
	    }

	    @Override
	    protected Relationship obtainSingleRelationship(final Node start, final Node end) {
	        return null;
	    }

	    @Override
	    protected Node getState(final Object entity) {
            return template.getPersistentState(entity);
	    }

	}
}
