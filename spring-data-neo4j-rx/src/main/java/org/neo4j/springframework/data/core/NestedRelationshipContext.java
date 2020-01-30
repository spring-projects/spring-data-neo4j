/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core;

import java.util.Map;

import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * Working on nested relationships happens in a certain algorithmic context.
 * This context enables a tight cohesion between the algorithmic steps and the data, these steps are performed on.
 * In our the interaction happens between the data that describes the relationship and the specific steps of
 * the algorithm.
 *
 * @author Philipp Tölle
 * @since 1.0
 */
final class NestedRelationshipContext {
	private final Neo4jPersistentProperty inverse;
	private final Object value;
	private final RelationshipDescription relationship;
	private final Class<?> associationTargetType;

	private final boolean inverseValueIsEmpty;

	private NestedRelationshipContext(Neo4jPersistentProperty inverse, Object value,
		RelationshipDescription relationship, Class<?> associationTargetType, boolean inverseValueIsEmpty) {
		this.inverse = inverse;
		this.value = value;
		this.relationship = relationship;
		this.associationTargetType = associationTargetType;
		this.inverseValueIsEmpty = inverseValueIsEmpty;
	}

	Neo4jPersistentProperty getInverse() {
		return inverse;
	}

	Object getValue() {
		return value;
	}

	RelationshipDescription getRelationship() {
		return relationship;
	}

	Class<?> getAssociationTargetType() {
		return associationTargetType;
	}

	public boolean inverseValueIsEmpty() {
		return inverseValueIsEmpty;
	}

	boolean hasRelationshipWithProperties() {
		return this.relationship.hasRelationshipProperties();
	}

	Object identifyAndExtractRelationshipValue(Object relatedValue) {
		Object valueToBeSaved = relatedValue;
		if (relatedValue instanceof Map.Entry) {
			Map.Entry relatedValueMapEntry = (Map.Entry) relatedValue;

			if (this.getInverse().isDynamicAssociation()) {
				valueToBeSaved = relatedValueMapEntry.getValue();
			} else if (this.hasRelationshipWithProperties()) {
				valueToBeSaved = relatedValueMapEntry.getKey();
			}
		}

		return valueToBeSaved;
	}

	static NestedRelationshipContext of(Association<Neo4jPersistentProperty> handler,
		PersistentPropertyAccessor<?> propertyAccessor,
		Neo4jPersistentEntity<?> neo4jPersistentEntity) {

		Neo4jPersistentProperty inverse = handler.getInverse();

		boolean inverseValueIsEmpty = propertyAccessor.getProperty(inverse) == null;
		Object value = propertyAccessor.getProperty(inverse);

		RelationshipDescription relationship = neo4jPersistentEntity
			.getRelationships().stream()
			.filter(r -> r.getFieldName().equals(inverse.getName()))
			.findFirst().get();

		// if we have a relationship with properties, the targetNodeType is the map key
		Class<?> associationTargetType = relationship.hasRelationshipProperties()
			? inverse.getComponentType()
			: inverse.getAssociationTargetType();

		return new NestedRelationshipContext(inverse, value, relationship, associationTargetType,
			inverseValueIsEmpty);
	}
}
