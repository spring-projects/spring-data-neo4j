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

import java.util.Objects;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;

import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Description of a relationship. Those descriptions always describe outgoing
 * relationships. The inverse direction is maybe defined on the {@link NodeDescription}
 * reachable in the {@link Schema} via it's primary label defined by {@link #getTarget}.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public interface RelationshipDescription {

	/**
	 * The name of the property SDN uses to transport relationship types.
	 */
	String NAME_OF_RELATIONSHIP = "__relationship__";

	/**
	 * The name of the property SDN uses to transport relationship types.
	 */
	String NAME_OF_RELATIONSHIP_TYPE = "__relationshipType__";

	/**
	 * If this relationship is dynamic, then this method always returns the name of the
	 * inverse property.
	 * @return the type of this relationship
	 */
	String getType();

	/**
	 * A relationship is dynamic when it's modelled as a {@code Map<String, ?>}.
	 * @return true, if this relationship is dynamic
	 */
	boolean isDynamic();

	/**
	 * The source of this relationship is described by the primary label of the node in
	 * question.
	 * @return the source of this relationship
	 */
	NodeDescription<?> getSource();

	/**
	 * The target of this relationship is described by the primary label of the node in
	 * question. If the relationship description includes a relationship properties class,
	 * this will be the {@link NodeDescription} of the
	 * {@link org.springframework.data.neo4j.core.schema.TargetNode}.
	 * @return the target of this relationship
	 */
	NodeDescription<?> getTarget();

	/**
	 * The name of the property where the relationship was defined. This is used by the
	 * Cypher creation to name the return values.
	 * @return the name of the field storing the relationship property
	 */
	String getFieldName();

	/**
	 * The direction of the defined relationship. This is used by the Cypher creation to
	 * query for relationships and create them with the right directions.
	 * @return the direction of the relationship
	 */
	Relationship.Direction getDirection();

	/**
	 * If this is a relationship with properties, the properties-defining class will get
	 * returned, otherwise {@literal null}.
	 * @return the type of the relationship property class for relationship with
	 * properties, otherwise {@literal null}
	 */
	@Nullable NodeDescription<?> getRelationshipPropertiesEntity();

	default NodeDescription<?> getRequiredRelationshipPropertiesEntity() {

		return Objects.requireNonNull(getRelationshipPropertiesEntity(),
				() -> "Relationship entity %s does not point to an entity holding the relationships' properties"
					.formatted(this.getType()));
	}

	/**
	 * Tells if this relationship is a relationship with additional properties. In such
	 * cases {@code getRelationshipPropertiesClass} will return the type of the properties
	 * holding class.
	 * @return {@literal true} if an additional properties are available, otherwise
	 * {@literal false}
	 */
	boolean hasRelationshipProperties();

	default boolean hasInternalIdProperty() {

		return hasRelationshipProperties() && Optional.ofNullable(getRelationshipPropertiesEntity())
			.map(NodeDescription::getIdDescription)
			.filter(IdDescription::isInternallyGeneratedId)
			.isPresent();
	}

	default boolean isOutgoing() {
		return Relationship.Direction.OUTGOING.equals(this.getDirection());
	}

	default boolean isIncoming() {
		return Relationship.Direction.INCOMING.equals(this.getDirection());
	}

	default String generateRelatedNodesCollectionName(NodeDescription<?> mostAbstractNodeDescription) {

		return this.getSource().getMostAbstractParentLabel(mostAbstractNodeDescription) + "_" + this.getType() + "_"
				+ this.getTarget().getPrimaryLabel() + "_" + this.isOutgoing();
	}

	/**
	 * Returns the logically same relationship definition in the target entity.
	 * @return logically same relationship definition in the target entity
	 */
	@Nullable RelationshipDescription getRelationshipObverse();

	/**
	 * Set the relationship definition that describes the opposite side of the
	 * relationship.
	 * @param relationshipObverse logically same relationship definition in the target
	 * entity
	 */
	void setRelationshipObverse(@Nullable RelationshipDescription relationshipObverse);

	/**
	 * Checks if there is a relationship description describing the obverse of this
	 * relationship.
	 * @return true if a logically same relationship in the target entity exists,
	 * otherwise false.
	 */
	boolean hasRelationshipObverse();

	/**
	 * Returns true if updates should be cascaded along this relationship.
	 * @return true if updates should be cascaded along this relationship
	 */
	boolean cascadeUpdates();

}
