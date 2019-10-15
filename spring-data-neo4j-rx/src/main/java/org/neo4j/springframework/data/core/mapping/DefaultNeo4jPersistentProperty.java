/*
 * Copyright (c) 2019 "Neo4j,"
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

import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Lazy;
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
			return !simpleTypeHolder.isSimpleType(targetType);
		});
		this.mappingContext = mappingContext;
	}

	@Override
	protected Association<Neo4jPersistentProperty> createAssociation() {

		Neo4jPersistentEntity<?> obverseOwner = this.mappingContext
			.getPersistentEntity(this.getAssociationTargetType());

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

		Neo4jPersistentProperty obverse = null;
		return new DefaultRelationshipDescription(this, obverse, type, this.isDynamicAssociation(),
			(NodeDescription<?>) getOwner(), this.getName(), obverseOwner, direction);
	}

	@Override
	public boolean isAssociation() {

		return this.isAssociation.orElse(false);
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
