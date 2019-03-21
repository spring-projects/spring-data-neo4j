/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

public class RelatedToCollectionFieldAccessorFactory implements FieldAccessorFactory {

    protected Neo4jTemplate template;

    public RelatedToCollectionFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        if (!property.isRelationship()) return false;
        final RelationshipInfo info = property.getRelationshipInfo();
        return info.isCollection() && info.isRelatedTo() && !info.isReadonly();
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final RelationshipInfo relationshipInfo = property.getRelationshipInfo();
        final Class<?> targetType = relationshipInfo.getTargetType().getType();
        return new RelatedToCollectionFieldAccessor(relationshipInfo.getRelationshipType(), relationshipInfo.getDirection(), targetType, template, property);
    }

    public static class RelatedToCollectionFieldAccessor extends RelatedToFieldAccessor {

        public RelatedToCollectionFieldAccessor(final RelationshipType type, final Direction direction, final Class<?> elementClass, final Neo4jTemplate template, Neo4jPersistentProperty property) {
            super(elementClass, template, direction, type, property);
        }

        public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            final Node node = checkAndGetNode(entity);
// null should not remove existing relationships but leave them alone
            if (newVal == null) return null;
            final Set<Node> targetNodes = createSetOfTargetNodes(newVal);
            removeMissingRelationships(node, targetNodes, property.getTargetType());
            createAddedRelationships(node, targetNodes);
            return createManagedSet(entity, (Set<?>) newVal, property.obtainMappingPolicy(mappingPolicy));
        }

        @Override
        public Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            checkAndGetNode(entity);
            final MappingPolicy currentPolicy = property.obtainMappingPolicy(mappingPolicy);
            final Set<?> result = property.isTargetTypeEnforced() ?
                    createEntitySetFromRelationshipEndNodesUsingTypeProperty(entity, currentPolicy) :
                    createEntitySetFromRelationshipEndNodes(entity, currentPolicy);

            Set<Object> values = new HashSet<Object>();
            Class<?> targetType = property.getTargetType();

            for (Object value : result) {
                if (targetType == null || targetType.isAssignableFrom(value.getClass())) {
                    values.add(value);
                }
            }

            return doReturn(createManagedSet(entity, values, currentPolicy));
        }

        @Override
        public Object getDefaultValue() {
            // todo delegate to property
            if (List.class.isAssignableFrom(property.getType())) return new ArrayList();
            return new HashSet();
        }
    }
}
