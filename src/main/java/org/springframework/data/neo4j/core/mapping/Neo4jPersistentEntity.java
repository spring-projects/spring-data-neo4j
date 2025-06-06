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
package org.springframework.data.neo4j.core.mapping;

import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * A {@link org.springframework.data.mapping.PersistentEntity} interface with additional
 * methods for metadata related to Neo4j. Both Spring Data methods
 * {@link #doWithProperties(PropertyHandler)} and
 * {@link #doWithAssociations(AssociationHandler)} are aware which field of a class is
 * meant to be mapped as a property of a node or a relationship or if it is a relationship
 * (in Spring Data terms: if it is an association).
 * <p>
 * <strong>Note</strong> to the outside world, we treat the
 * {@link org.springframework.data.neo4j.core.schema.TargetNode @TargetNode} annotated
 * field of a
 * {@link org.springframework.data.neo4j.core.schema.RelationshipProperties @RelationshipProperties}
 * annotated class as association. Internally, we treat it as a property
 *
 * @param <T> type of the underlying class
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPersistentEntity<T>
		extends MutablePersistentEntity<T, Neo4jPersistentProperty>, NodeDescription<T> {

	/**
	 * Types that are bound to Neo4j id() and are officially deprecated from Neo4j.
	 */
	Set<Class<?>> DEPRECATED_GENERATED_ID_TYPES = Set.of(Long.class, long.class);

	/**
	 * A collection liked property containing labels to be stored dynamically with this
	 * entity.
	 * @return an optional property pointing to a {@link java.util.Collection
	 * Collection&lt;String&gt;} containing dynamic "runtime managed" labels.
	 */
	Optional<Neo4jPersistentProperty> getDynamicLabelsProperty();

	/**
	 * Determines if the entity is annotated with
	 * {@link org.springframework.data.neo4j.core.schema.RelationshipProperties}.
	 * @return true if this is a relationship properties class, otherwise false.
	 */
	boolean isRelationshipPropertiesEntity();

	/**
	 * Determines if the entity is annotated with
	 * {@link org.springframework.data.neo4j.core.schema.RelationshipProperties} and has
	 * the flag
	 * {@link org.springframework.data.neo4j.core.schema.RelationshipProperties#persistTypeInfo()}
	 * set to true.
	 * @return true if this is a relationship properties class and the type info should be
	 * persisted, otherwise false.
	 */
	boolean hasRelationshipPropertyPersistTypeInfoFlag();

	/**
	 * Checks if this entity is using deprecated internal ids anywhere in its hierarchy.
	 * @return true if the underlying domain classes uses {@code id()} to compute
	 * internally generated ids.
	 */
	default boolean isUsingDeprecatedInternalId() {
		for (NodeDescription<?> nodeDescription : getChildNodeDescriptionsInHierarchy()) {
			if (nodeDescription.isUsingInternalIds()
					&& ((Neo4jPersistentEntity<?>) nodeDescription).getIdProperty() != null
					&& Neo4jPersistentEntity.DEPRECATED_GENERATED_ID_TYPES
						.contains(((Neo4jPersistentEntity<?>) nodeDescription).getIdProperty().getType())) {
				return true;
			}
		}
		return isUsingInternalIds()
				&& Neo4jPersistentEntity.DEPRECATED_GENERATED_ID_TYPES.contains(getRequiredIdProperty().getType());
	}

	/**
	 * Returns true if this entity spots a vector property.
	 * @return true if this entity spots a vector property
	 */
	boolean hasVectorProperty();

	/**
	 * Will return the single supported vector property if {@link #hasVectorProperty()}
	 * returns {@literal true}, otherwise {@literal null}.
	 * @return an optional vector property on this entity
	 */
	@Nullable Neo4jPersistentProperty getVectorProperty();

	/**
	 * Will return the single supported vector property if {@link #hasVectorProperty()}
	 * returns {@literal true}, otherwise it will throw an {@link IllegalStateException}.
	 * @return the vector property on this entity.
	 */
	Neo4jPersistentProperty getRequiredVectorProperty();

}
