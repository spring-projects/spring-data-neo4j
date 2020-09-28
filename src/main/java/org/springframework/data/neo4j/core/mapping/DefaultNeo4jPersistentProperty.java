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
package org.springframework.data.neo4j.core.mapping;

import java.util.Optional;
import java.util.function.Function;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.util.Lazy;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @since 6.0
 */
final class DefaultNeo4jPersistentProperty extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty>
		implements Neo4jPersistentProperty {

	private final Lazy<String> graphPropertyName;
	private final Lazy<Boolean> isAssociation;
	private final boolean isEntityInRelationshipWithProperties;

	private final Neo4jMappingContext mappingContext;

	private final Lazy<Neo4jPersistentPropertyConverter> customConversion;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 *
	 * @param property         must not be {@literal null}.
	 * @param owner            must not be {@literal null}.
	 * @param simpleTypeHolder type holder
	 */
	DefaultNeo4jPersistentProperty(Property property, PersistentEntity<?, Neo4jPersistentProperty> owner,
			Neo4jMappingContext mappingContext, SimpleTypeHolder simpleTypeHolder,
			boolean isEntityInRelationshipWithProperties) {

		super(property, owner, simpleTypeHolder);
		this.isEntityInRelationshipWithProperties = isEntityInRelationshipWithProperties;
		this.mappingContext = mappingContext;

		this.graphPropertyName = Lazy.of(this::computeGraphPropertyName);
		this.isAssociation = Lazy.of(() -> {

			Class<?> targetType = getActualType();
			return !(simpleTypeHolder.isSimpleType(targetType) || this.mappingContext.hasCustomWriteTarget(targetType)
					|| isAnnotationPresent(TargetNode.class));
		});

		this.customConversion = Lazy.of(() -> {

			if (this.isEntity()) {
				return null;
			}

			return this.mappingContext.getOptionalCustomConversionsFor(this);
		});
	}

	@Override
	protected Association<Neo4jPersistentProperty> createAssociation() {

		Neo4jPersistentEntity<?> obverseOwner;

		// if the target is a relationship property always take the key type from the map instead of the value type.
		if (this.hasActualTypeAnnotation(RelationshipProperties.class)) {
			Class<?> type = this.mappingContext.getPersistentEntity(getActualType()).getPersistentProperty(TargetNode.class).getType();
			obverseOwner = this.mappingContext.getPersistentEntity(type);
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
		Neo4jPersistentEntity<?> relationshipPropertiesClass =
				this.hasActualTypeAnnotation(RelationshipProperties.class)
						? this.mappingContext.getPersistentEntity(getActualType())
						: null;

		// Try to determine if there is a relationship definition that expresses logically the same relationship
		// on the other end.
		Optional<RelationshipDescription> obverseRelationshipDescription = obverseOwner.getRelationships().stream()
				.filter(rel -> rel.getType().equals(type) && rel.getTarget().equals(this.getOwner())).findFirst();

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
		return super.isEntity() && isAssociation() || (super.isEntity() && isEntityInRelationshipWithProperties());
	}

	private static Function<Object, Value> nullSafeWrite(Function<Object, Value> delegate) {
		return source -> source == null ? Values.NULL : delegate.apply(source);
	}

	@Override
	public Function<Object, Value> getOptionalWritingConverter() {
		return customConversion.getOptional().map(c -> nullSafeWrite(c::write)).orElse(null);
	}

	private static Function<Value, Object> nullSafeRead(Function<Value, Object> delegate) {
		return source -> source == null || source.isNull() ? null : delegate.apply(source);
	}

	@Override
	public Function<Value, Object> getOptionalReadingConverter() {
		return customConversion.getOptional().map(c -> nullSafeRead(c::read)).orElse(null);
	}

	@Override
	public boolean isEntityInRelationshipWithProperties() {
		return isEntityInRelationshipWithProperties;
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

		org.springframework.data.neo4j.core.schema.Property propertyAnnotation = this
				.findAnnotation(org.springframework.data.neo4j.core.schema.Property.class);

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
			throw new MappingException("The property '" + propertyName + "' is not mapped to a Graph property!");
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
