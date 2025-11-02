/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core.mapping;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Interned;
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
	 * Checks if this property should use FalkorDB's intern() function.
	 * @return true if this property is marked with @Interned
	 */
	@Override
	public final boolean isInterned() {
		return isAnnotationPresent(Interned.class);
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
