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
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Set;

/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public abstract class RelatedToFieldAccessor implements FieldAccessor {
    protected final RelationshipType type;
    protected final Neo4jPersistentProperty property;
    protected final Direction direction;
    protected final Class<?> relatedType;
    protected final Neo4jTemplate template;
    protected RelationshipHelper relationshipHelper;

    public RelatedToFieldAccessor(Class<?> relatedType, Neo4jTemplate template, Direction direction, RelationshipType type, Neo4jPersistentProperty property) {
        this.relationshipHelper = new RelationshipHelper(template, direction, type);
        this.relatedType = relatedType;
        this.template = template;
        this.direction = direction;
        this.type = type;
        this.property = property;
    }

    @Override
    public boolean isWriteable(Object entity) {
        return true;
    }

    protected <T> ManagedFieldAccessorSet<T> createManagedSet(Object entity, Set<T> result, MappingPolicy mappingPolicy) {
        return ManagedFieldAccessorSet.create(entity, result, mappingPolicy, property, template, this);
    }

    public Object getDefaultValue() {
        return null;
    }

    // delegating methods

    protected Node checkAndGetNode(Object entity) {
        return relationshipHelper.checkAndGetNode(entity);
    }

    protected void removeMissingRelationships(Node node, Set<Node> targetNodes) {
        relationshipHelper.removeMissingRelationshipsInStoreAndKeepOnlyNewRelationShipsInSet(node, targetNodes);
    }

    protected void createAddedRelationships(Node node, Set<Node> targetNodes) {
        relationshipHelper.createAddedRelationships(node, targetNodes);
    }

    protected Set<Node> createSetOfTargetNodes(Object newVal) {
        return relationshipHelper.createSetOfTargetNodes(newVal, relatedType);
    }

    protected Set<Object> createEntitySetFromRelationshipEndNodes(Object entity, MappingPolicy mappingPolicy) {
        return relationshipHelper.createEntitySetFromRelationshipEndNodes(entity, mappingPolicy, relatedType);
    }
}
