/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.ExposesRelationships;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.RelationshipPattern;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.TargetNode;

class PropertyPathWrapper {
	private static final String NAME_OF_RELATED_FILTER_ENTITY = "m";
	private static final String NAME_OF_RELATED_FILTER_RELATIONSHIP = "r";

	private final int index;
	private final PersistentPropertyPath<?> persistentPropertyPath;
	private final int lengthModification;

	PropertyPathWrapper(int index, PersistentPropertyPath<?> persistentPropertyPath) {
		this(index, persistentPropertyPath, true);
	}

	PropertyPathWrapper(int index, PersistentPropertyPath<?> persistentPropertyPath, boolean hasPropertyEnding) {
		this.index = index;
		this.persistentPropertyPath = persistentPropertyPath;
		this.lengthModification = hasPropertyEnding ? 0 : 1;
	}

	public PersistentPropertyPath<?> getPersistentPropertyPath() {
		return persistentPropertyPath;
	}

	String getNodeName() {
		return NAME_OF_RELATED_FILTER_ENTITY + "_" + index;
	}

	String getRelationshipName() {
		return NAME_OF_RELATED_FILTER_RELATIONSHIP + "_" + index;
	}

	ExposesRelationships<?> createRelationshipChain(ExposesRelationships<?> existingRelationshipChain) {

		ExposesRelationships<?> cypherRelationship = existingRelationshipChain;
		int cnt = 0;
		for (PersistentProperty<?> persistentProperty : persistentPropertyPath) {

			if (persistentProperty.isAssociation() && persistentProperty.isAnnotationPresent(TargetNode.class)) {
				break;
			}

			RelationshipDescription relationshipDescription = (RelationshipDescription) persistentProperty.getAssociation();

			if (relationshipDescription == null) {
				break;
			}

			NodeDescription<?> relationshipPropertiesEntity = relationshipDescription.getRelationshipPropertiesEntity();
			boolean isRelationshipPropertiesEntity = isRelationshipPropertiesEntity(relationshipPropertiesEntity);

			NodeDescription<?> targetEntity = relationshipDescription.getTarget();
			Node relatedNode = Cypher.node(targetEntity.getPrimaryLabel(), targetEntity.getAdditionalLabels());

			// length - 1 = last index
			// length - 2 = property on last node
			// length - 3 = last node itself
			// length + 1 if there is no property ending but the path only goes until it reaches the relationship field
			boolean lastNode = cnt > (persistentPropertyPath.getLength() - 3 + lengthModification);
			boolean lastRelationship = cnt + 1 > (persistentPropertyPath.getLength() - 4 + lengthModification);
			cnt = cnt + 1;

			// we don't yet if the condition will target a relationship property
			// that's why here is lastNode or any relationship property
			if (lastNode || (isRelationshipPropertiesEntity && lastRelationship)) {
				relatedNode = relatedNode.named(getNodeName());
			}

			cypherRelationship = switch (relationshipDescription.getDirection()) {
				case OUTGOING -> cypherRelationship
						.relationshipTo(relatedNode, relationshipDescription.getType());
				case INCOMING -> cypherRelationship
						.relationshipFrom(relatedNode, relationshipDescription.getType());
			};

			if (lastNode || (isRelationshipPropertiesEntity && lastRelationship)) {
				cypherRelationship = ((RelationshipPattern) cypherRelationship).named(getRelationshipName());
			}
		}

		return cypherRelationship;
	}

	private boolean isRelationshipPropertiesEntity(@Nullable NodeDescription<?> relationshipPropertiesEntity) {
		return relationshipPropertiesEntity != null
				&& ((Neo4jPersistentEntity<?>) relationshipPropertiesEntity)
				.getPersistentProperty(TargetNode.class) != null;
	}

	// if there is no direct property access, the list size is greater than 1 and as a consequence has to contain
	// relationships.
	boolean hasRelationships() {
		return this.persistentPropertyPath.getLength() > 1;
	}
}
