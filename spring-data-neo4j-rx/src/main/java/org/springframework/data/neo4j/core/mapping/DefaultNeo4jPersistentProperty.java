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
package org.springframework.data.neo4j.core.mapping;

import java.util.Optional;

import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.Lazy;

/**
 * @author Michael J. Simons
 */
class DefaultNeo4jPersistentProperty extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty>
	implements Neo4jPersistentProperty {

	private final Lazy<Optional<String>> graphPropertyName;

	/**
	 * Creates a new {@link AnnotationBasedPersistentProperty}.
	 *
	 * @param property         must not be {@literal null}.
	 * @param owner            must not be {@literal null}.
	 * @param simpleTypeHolder
	 */
	DefaultNeo4jPersistentProperty(Property property,
		PersistentEntity<?, Neo4jPersistentProperty> owner,
		SimpleTypeHolder simpleTypeHolder) {

		super(property, owner, simpleTypeHolder);

		this.graphPropertyName = Lazy.of(() -> computeGraphPropertyName());
	}

	@Override
	protected Association<Neo4jPersistentProperty> createAssociation() {

		return new Association<>(this, null);
	}

	@Override
	public boolean isAssociation() {
		return !SimpleTypeHolder.DEFAULT.isSimpleType(super.getType());
	}

	/**
	 * Computes the target name of this property.
	 *
	 * @return An empty optional if this property describes an association, a given target name otherwise.
	 */
	Optional<String> computeGraphPropertyName() {

		if (this.isAssociation()) {
			return Optional.empty();
		}

		org.springframework.data.neo4j.core.schema.Property propertyAnnotation =
			this.findAnnotation(org.springframework.data.neo4j.core.schema.Property.class);

		String targetName = this.getName();
		if (propertyAnnotation != null && !propertyAnnotation.name().isEmpty()
			&& propertyAnnotation.name().trim().length() != 0) {
			targetName = propertyAnnotation.name().trim();
		}

		return Optional.of(targetName);
	}

	@Override
	public String getFieldName() {
		return this.getName();
	}

	@Override
	public String getPropertyName() {
		return this.graphPropertyName.get().orElseThrow(() -> new MappingException("This property is not mapped to a Graph property!"));
	}
}
