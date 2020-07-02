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
package org.neo4j.springframework.data.core.mapping;

import java.util.Optional;

import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.neo4j.springframework.data.core.schema.RelationshipProperties;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @since 1.0
 */
class DefaultNeo4jPersistentProperty extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty>
	implements Neo4jPersistentProperty {

	private final Lazy<String> graphPropertyName;
	private final Lazy<Boolean> isAssociation;

	private final Neo4jMappingContext mappingContext;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 *
	 * @param property         must not be {@literal null}.
	 * @param owner            must not be {@literal null}.
	 * @param simpleTypeHolder type holder
	 */
	DefaultNeo4jPersistentProperty(Property property,
		PersistentEntity<?, Neo4jPersistentProperty> owner,
		Neo4jMappingContext mappingContext,
		SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.graphPropertyName = Lazy.of(this::computeGraphPropertyName);
		this.isAssociation = Lazy.of(() -> {

			Class<?> targetType = getActualType();
			return !(simpleTypeHolder.isSimpleType(targetType) || mappingContext.hasCustomWriteTarget(targetType));
		});
		this.mappingContext = mappingContext;
	}

	@Override
	protected Association<Neo4jPersistentProperty> createAssociation() {

		Neo4jPersistentEntity<?> obverseOwner;

		// if the target is a relationship property always take the key type from the map instead of the value type.
		if (this.hasActualTypeAnnotation(RelationshipProperties.class)) {
			obverseOwner = this.mappingContext.getPersistentEntity(this.getComponentType());
		} else {
			obverseOwner = this.mappingContext.getPersistentEntity(this.getAssociationTargetType());
		}

		Relationship outgoingRelationship = this.findAnnotation(Relationship.class);

		String type;
		if (outgoingRelationship != null && outgoingRelationship.type() != null) {
			type = outgoingRelationship.type();
		} else {
			type = deriveRelationshipType(this.getName());
		}

		Relationship.Direction direction = Relationship.Direction.OUTGOING;
		if (outgoingRelationship != null) {
			direction = outgoingRelationship.direction();
		}

		boolean dynamicAssociation = this.isDynamicAssociation();

		// Because a dynamic association is also represented as a Map, this ensures that the
		// relationship properties class will only have a value if it's not a dynamic association.
		Class<?> relationshipPropertiesClass = dynamicAssociation ? null : getMapValueType();

		// Try to determine if there is a relationship definition that expresses logically the same relationship
		// on the other end.
		Optional<RelationshipDescription> obverseRelationshipDescription = obverseOwner.getRelationships().stream()
			.filter(rel -> rel.getType().equals(type) && rel.getTarget().equals(this.getOwner()))
			.findFirst();

		DefaultRelationshipDescription relationshipDescription = new DefaultRelationshipDescription(this,
			obverseRelationshipDescription.orElse(null), type, dynamicAssociation, (NodeDescription<?>) getOwner(),
			this.getName(), obverseOwner, direction, relationshipPropertiesClass);

		// Update the previous found, if any, relationship with the newly created one as its counterpart.
		obverseRelationshipDescription
			.ifPresent(relationship -> relationship.setRelationshipObverse(relationshipDescription));

		return relationshipDescription;
	}

	@Override
	public Class<?> getAssociationTargetType() {

		Class<?> associationTargetType = super.getAssociationTargetType();
		if (associationTargetType != null) {
			return associationTargetType;
		} else if (isDynamicOneToManyAssociation()) {
			TypeInformation<?> actualType = getTypeInformation().getRequiredActualType();
			return actualType.getRequiredComponentType().getType();
		} else {
			return null;
		}
	}

	@Override
	public boolean isAssociation() {

		return this.isAssociation.orElse(false);
	}

	@Override
	public boolean isEntity() {
		return super.isEntity() && isAssociation();
	}

	/**
	 * Computes the target name of this property.
	 *
	 * @return A property on a node or {@literal null} if this property describes an association.
	 */
	@Nullable
	private String computeGraphPropertyName() {

		if (this.isAssociation()) {
			return null;
		}

		org.neo4j.springframework.data.core.schema.Property propertyAnnotation =
			this.findAnnotation(org.neo4j.springframework.data.core.schema.Property.class);

		String targetName = this.getName();
		if (propertyAnnotation != null && !propertyAnnotation.name().isEmpty()
			&& propertyAnnotation.name().trim().length() != 0) {
			targetName = propertyAnnotation.name().trim();
		}

		return targetName;
	}

	@Override
	public String getFieldName() {
		return this.getName();
	}

	@Override
	public String getPropertyName() {

		String propertyName = this.graphPropertyName.getNullable();
		if (propertyName == null) {
			throw new MappingException("This property is not mapped to a Graph property!");
		}

		return propertyName;
	}

	@Override
	public boolean isInternalIdProperty() {

		return this.isIdProperty() && ((Neo4jPersistentEntity) this.getOwner()).isUsingInternalIds();
	}

	@Override
	public boolean isRelationship() {

		return isAssociation();
	}


	static String deriveRelationshipType(String name) {

		Assert.hasText(name, "The name to derive the type from is required.");

		StringBuilder sb = new StringBuilder();

		int codePoint;
		int previousIndex = 0;
		int i = 0;
		while (i < name.length()) {
			codePoint = name.codePointAt(i);
			if (Character.isLowerCase(codePoint)) {
				if (i > 0 && !Character.isLetter(name.codePointAt(previousIndex))) {
					sb.append("_");
				}
				codePoint = Character.toUpperCase(codePoint);
			} else if (sb.length() > 0) {
				sb.append("_");
			}
			sb.append(Character.toChars(codePoint));
			previousIndex = i;
			i += Character.charCount(codePoint);
		}
		return sb.toString();
	}
}
