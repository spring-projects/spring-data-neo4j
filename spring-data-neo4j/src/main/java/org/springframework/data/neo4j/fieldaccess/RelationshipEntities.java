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

import org.neo4j.graphdb.Node;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 28.02.12
 */
class RelationshipEntities {

    private final RelationshipHelper relationshipHelper;
    private final Neo4jPersistentProperty property;
    private final RelationshipProperties relationshipProperties;
    private final Neo4jPersistentProperty endNodeProperty;
    private final Neo4jPersistentProperty startNodeProperty;
    private final MappingPolicy endNodeMappingPolicy;
    private final MappingPolicy startNodeMappingPolicy;

    public RelationshipEntities(RelationshipHelper relationshipHelper, Neo4jPersistentProperty property) {
        this.relationshipHelper = relationshipHelper;
        this.property = property;
        relationshipProperties = property.getRelationshipInfo().getTargetEntity().getRelationshipProperties();
        endNodeProperty = relationshipProperties.getEndNodeProperty();
        startNodeProperty = relationshipProperties.getStartNodeProperty();
        endNodeMappingPolicy = endNodeProperty.getMappingPolicy();
        startNodeMappingPolicy = startNodeProperty.getMappingPolicy();
    }

    public Node getOtherNode(Node startNode, Object relationshipEntity) {
        Object fieldValue = endNodeProperty.getValue(relationshipEntity, endNodeMappingPolicy);

        if (fieldValue == null)
            throw new IllegalArgumentException("End node must not be null (" + relationshipEntity.getClass().getName() + ")");

        final Node endNode = relationshipHelper.getNode(fieldValue);

        if (startNode.equals(endNode)) {
            return relationshipHelper.getNode(startNodeProperty.getValue(relationshipEntity, startNodeMappingPolicy));
        } else {
            return endNode;
        }
    }

    public Map<Node, Object> loadEndNodeToRelationshipEntityMapping(Node startNode, Iterable<Object> values, Class<?> relatedType) {
        Map<Node, Object> endNodeToEntityMapping = new HashMap<Node, Object>();
        for (Object entry : values) {
            if (!relatedType.isInstance(entry))
                throw new IllegalArgumentException("Elements of " + property + " collection must be of " + relatedType);
            Node endNode = getOtherNode(startNode, entry);
            endNodeToEntityMapping.put(endNode, entry);
        }
        return endNodeToEntityMapping;
    }
}
