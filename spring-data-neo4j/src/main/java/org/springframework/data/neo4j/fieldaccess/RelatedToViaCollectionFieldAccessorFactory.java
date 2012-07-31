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
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class RelatedToViaCollectionFieldAccessorFactory implements FieldAccessorFactory {

	private Neo4jTemplate template;

	public RelatedToViaCollectionFieldAccessorFactory(Neo4jTemplate template) {
		this.template = template;
	}

	@Override
	public boolean accept(final Neo4jPersistentProperty property) {
        if (!property.isRelationship()) return false;
        return property.getRelationshipInfo().isRelatedToVia() && property.getRelationshipInfo().isCollection();
    }

	@Override
	public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
		return new RelatedToViaCollectionFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), (Class<?>) relationshipInfo.getTargetType().getType(), template,property);
	}

	public static class RelatedToViaCollectionFieldAccessor implements FieldAccessor {

        private final boolean isMutableCollection;
        private final Class<?> relatedType;
        private final Neo4jTemplate template;
        private final Neo4jPersistentProperty property;
        private final RelationshipHelper relationshipHelper;
        private final RelationshipEntities relationshipEntities;

        public RelatedToViaCollectionFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> relatedType, final Neo4jTemplate template, Neo4jPersistentProperty property) {
            relationshipHelper = new RelationshipHelper(template, direction, type);
            this.relatedType = relatedType;
            this.template = template;
            this.property = property;
            isMutableCollection = Collection.class.isAssignableFrom(property.getType());
            relationshipEntities = new RelationshipEntities(relationshipHelper, property);
        }

        @Override
        public Object getDefaultValue() {
            // todo delegate to property
            if (List.class.isAssignableFrom(property.getType())) return new ArrayList();
            return new HashSet();
        }

        @Override
	    public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
	        if (!isMutableCollection) throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
            final Node startNode = relationshipHelper.checkAndGetNode(entity);
            // null collections values are ignored, not deleting relationships
            if (newVal == null) return null;

            final Map<Node, Object> endNodeToEntityMapping = loadEndNodeToRelationshipEntityMapping(newVal, startNode);
            relationshipHelper.removeMissingRelationshipsInStoreAndKeepOnlyNewRelationShipsInSet(startNode, endNodeToEntityMapping.keySet(), null);
            persistEntities(endNodeToEntityMapping.values(), relationshipHelper.getRelationshipType());
            return createManagedSet(entity, (Set<?>) newVal, property.obtainMappingPolicy(mappingPolicy));
	    }

        private void persistEntities( final Collection<Object> relationshipEntities, RelationshipType relationshipType ) {
            for (Object entity : relationshipEntities) {
                template.save(entity, relationshipType);
            }
        }

        protected Map<Node, Object> loadEndNodeToRelationshipEntityMapping(Object newVal, Node startNode) {
            if (!(newVal instanceof Set)) {
                throw new IllegalArgumentException("New value must be at least an Iterable, was: " + newVal.getClass());
            }
            return relationshipEntities.loadEndNodeToRelationshipEntityMapping(startNode, (Iterable<Object>) newVal, relatedType);
        }


        @Override
	    public boolean isWriteable(Object entity) {
	        return isMutableCollection;
	    }

	    @Override
	    public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            final Node node = relationshipHelper.checkAndGetNode(entity);
            final GraphBackedEntityIterableWrapper<Relationship, ?> result = loadRelationshipEntities(node);
            if (isMutableCollection) {
                @SuppressWarnings("unchecked") final ManagedFieldAccessorSet managedSet = createManagedSet(entity, IteratorUtil.addToCollection(result, new HashSet()), property.obtainMappingPolicy(mappingPolicy));
                return doReturn(managedSet);
            }
            return doReturn(result);
        }

        protected <T> ManagedFieldAccessorSet<T> createManagedSet(Object entity, Set<T> result, MappingPolicy mappingPolicy) {
                return ManagedFieldAccessorSet.create(entity, result, mappingPolicy, property, template, this);
        }

        private GraphBackedEntityIterableWrapper<Relationship, ?> loadRelationshipEntities(final Node node) {
            return GraphBackedEntityIterableWrapper.create(relationshipHelper.getRelationships(node), relatedType, template);
        }
    }
}
