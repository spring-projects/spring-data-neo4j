/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.schema.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.support.Relationships;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * Working on nested relationships happens in a certain algorithmic context. This context enables a tight cohesion
 * between the algorithmic steps and the data, these steps are performed on. In our the interaction happens between the
 * data that describes the relationship and the specific steps of the algorithm.
 *
 * @author Philipp Tölle
 * @author Gerrit Meier
 * @since 6.0
 */
final class NestedRelationshipContext {
	private final Neo4jPersistentProperty inverse;
	private final Object value;
	private final Object relationshipProperties;
	private final RelationshipDescription relationship;
	private final Class<?> associationTargetType;

	private final boolean inverseValueIsEmpty;

	private NestedRelationshipContext(Neo4jPersistentProperty inverse, @Nullable Object value, @Nullable Object relationshipProperties,
			RelationshipDescription relationship, Class<?> associationTargetType, boolean inverseValueIsEmpty) {
		this.inverse = inverse;
		this.value = value;
		this.relationshipProperties = relationshipProperties;
		this.relationship = relationship;
		this.associationTargetType = associationTargetType;
		this.inverseValueIsEmpty = inverseValueIsEmpty;
	}

	Neo4jPersistentProperty getInverse() {
		return inverse;
	}

	@Nullable
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

	public Object getRelationshipProperties() {
		return relationshipProperties;
	}

	Object identifyAndExtractRelationshipTargetNode(Object relatedValue) {
		Object valueToBeSaved = relatedValue;
		if (relatedValue instanceof Map.Entry) {
			Map.Entry relatedValueMapEntry = (Map.Entry) relatedValue;

			if (this.getInverse().isDynamicAssociation()) {
				valueToBeSaved = relatedValueMapEntry.getValue();
			}
		}
		if (this.hasRelationshipWithProperties()) {
			// here comes the entity
			valueToBeSaved = ((Relationships.RelationshipWithProperties) relatedValue).relatedEntity;
		}

		return valueToBeSaved;
	}

	static NestedRelationshipContext of(Association<Neo4jPersistentProperty> handler,
			PersistentPropertyAccessor<?> propertyAccessor, Neo4jPersistentEntity<?> neo4jPersistentEntity) {

		Neo4jPersistentProperty inverse = handler.getInverse();

		boolean inverseValueIsEmpty = propertyAccessor.getProperty(inverse) == null;
		// value can be a collection or scalar of related notes, point to a relationship property (scalar or collection)
		// or is a dynamic relationship (map)
		Object value = propertyAccessor.getProperty(inverse);

		RelationshipDescription relationship = neo4jPersistentEntity.getRelationships().stream()
				.filter(r -> r.getFieldName().equals(inverse.getName())).findFirst().get();

		// if we have a relationship with properties, the targetNodeType is the map key
		Class<?> associationTargetType = relationship.hasRelationshipProperties() ? inverse.getComponentType()
				: inverse.getAssociationTargetType();

		Object relationshipProperties = null;
		if (relationship.hasRelationshipProperties() && value != null) {
			List<Relationships.RelationshipWithProperties> allOfThem = new ArrayList<>();

			for (Object relationshipWithProperties : ((Collection) value)) {

				Relationships.RelationshipWithProperties oneOfThem = new Relationships.RelationshipWithProperties(relationshipWithProperties, getTargetNode(relationshipWithProperties));
				allOfThem.add(oneOfThem);
			}
			value = allOfThem;
		}

		return new NestedRelationshipContext(inverse, value, relationshipProperties, relationship, associationTargetType, inverseValueIsEmpty);
	}

	private static Object getTargetNode(Object object) {
		Class<?> objectClass = object.getClass();
		for (Field field : objectClass.getDeclaredFields()) {
			if (field.isAnnotationPresent(TargetNode.class)) {
				try {
					ReflectionUtils.makeAccessible(field);
					return field.get(object);
				} catch (IllegalAccessException e) {
					throw new RuntimeException("Kaputt");
				}
			}
		}
		return null;
	}
}
