/*
 * Copyright 2011-present the original author or authors.
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

import org.apiguardian.api.API;
import org.springframework.data.domain.Vector;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.schema.CompositeProperty;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.lang.Nullable;

/**
 * A {@link org.springframework.data.mapping.PersistentProperty} interface with additional methods for metadata related
 * to Neo4j.
 *
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @author Gerrit Meier
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPersistentProperty extends PersistentProperty<Neo4jPersistentProperty>, GraphPropertyDescription {

	/**
	 * Dynamic associations are associations to non-simple types stored in a map with a key type of
	 * {@literal java.lang.String} or enum.
	 *
	 * @return True, if this association is a dynamic association.
	 */
	default boolean isDynamicAssociation() {

		return isRelationship() && isMap() && (getComponentType() == String.class || getComponentType().isEnum());
	}

	/**
	 * Dynamic one-to-many associations are associations to non-simple types stored in a map with a key type of
	 * {@literal java.lang.String} and values of {@literal java.util.Collection}.
	 *
	 * @return True, if this association is a dynamic association with multiple values per type.
	 * @since 6.0.1
	 */
	default boolean isDynamicOneToManyAssociation() {

		return this.isDynamicAssociation() && getTypeInformation().getRequiredActualType().isCollectionLike();
	}

	/**
	 * @return whether the property is a property describing dynamic labels
	 * @since 6.0
	 */
	default boolean isDynamicLabels() {
		return this.isAnnotationPresent(DynamicLabels.class) && this.isCollectionLike();
	}

	default boolean isVectorProperty() {
		return Vector.class.isAssignableFrom(this.getType());
	}

	@Nullable
	Neo4jPersistentPropertyConverter<?> getOptionalConverter();

	/**
	 * @return True if this property targets an entity which is a container for relationship properties.
	 */
	boolean isEntityWithRelationshipProperties();

	/**
	 * Computes a prefix to be used on multiple properties on a node when this persistent property is annotated with
	 * {@link CompositeProperty @CompositeProperty}.
	 *
	 * @return A valid prefix
	 */
	default String computePrefixWithDelimiter() {
		CompositeProperty compositeProperty = getRequiredAnnotation(CompositeProperty.class);
		return Optional.of(compositeProperty.prefix()).map(String::trim).filter(s -> !s.isEmpty())
				.orElseGet(this::getFieldName) + compositeProperty.delimiter();
	}

	/**
	 * @return {@literal true} if this is a read only property.
	 */
	default boolean isReadOnly() {
		return false;
	}
}
