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
package org.springframework.data.falkordb.core.mapping;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link FalkorDBPersistentProperty}.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class DefaultFalkorDBPersistentProperty extends AnnotationBasedPersistentProperty<FalkorDBPersistentProperty>
		implements FalkorDBPersistentProperty {

	/**
	 * Creates a new {@link DefaultFalkorDBPersistentProperty}.
	 * @param property must not be {@literal null}.
	 * @param owner must not be {@literal null}.
	 * @param simpleTypeHolder must not be {@literal null}.
	 */
	public DefaultFalkorDBPersistentProperty(org.springframework.data.mapping.model.Property property,
			FalkorDBPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		super(property, owner, simpleTypeHolder);
	}

	@Override
	public String getGraphPropertyName() {
		Property propertyAnnotation = findAnnotation(Property.class);

		if (propertyAnnotation != null) {
			if (StringUtils.hasText(propertyAnnotation.name())) {
				return propertyAnnotation.name();
			}
			else if (StringUtils.hasText(propertyAnnotation.value())) {
				return propertyAnnotation.value();
			}
		}

		// Fallback to field name
		return getName();
	}

	/**
	 * Checks if this property is a relationship.
	 * @return true if this is a relationship property
	 */
	@Override
	public final boolean isRelationship() {
		return isAnnotationPresent(Relationship.class);
	}

	/**
	 * Checks if this property is an internal ID property.
	 * @return true if this is an internal ID property
	 */
	@Override
	public final boolean isInternalIdProperty() {
		return isIdProperty() && isGeneratedValue() && (getType().equals(Long.class) || getType().equals(long.class));
	}

	/**
	 * Checks if this property has a generated value.
	 * @return true if this property has a generated value
	 */
	@Override
	public final boolean isGeneratedValue() {
		return isAnnotationPresent(GeneratedValue.class);
	}

	/**
	 * Gets the relationship type for this property.
	 * @return the relationship type or null
	 */
	@Override
	public final String getRelationshipType() {
		Relationship relationshipAnnotation = findAnnotation(Relationship.class);

		if (relationshipAnnotation != null) {
			if (StringUtils.hasText(relationshipAnnotation.type())) {
				return relationshipAnnotation.type();
			}
			else if (StringUtils.hasText(relationshipAnnotation.value())) {
				return relationshipAnnotation.value();
			}
		}

		return null;
	}

	/**
	 * Creates an association for this property.
	 * @return the association
	 */
	@Override
	protected final Association<FalkorDBPersistentProperty> createAssociation() {
		return new Association<>(this, null);
	}

}
