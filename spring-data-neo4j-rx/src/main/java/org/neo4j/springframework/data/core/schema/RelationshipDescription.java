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
package org.neo4j.springframework.data.core.schema;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.neo4j.springframework.data.core.schema.Relationship.Direction;

/**
 * Description of a relationship. Those descriptions always describe outgoing relationships. The inverse direction
 * is maybe defined on the {@link NodeDescription} reachable in the {@link Schema} via it's primary label defined by
 * {@link #getTarget}.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface RelationshipDescription {

	String NAME_OF_RELATIONSHIP_TYPE = "__relationshipType__";

	/**
	 * If this relationship is dynamic, than this method always returns the name of the inverse property.
	 *
	 * @return The type of this relationship
	 */
	String getType();

	/**
	 * A relationship is dynamic when it's modelled as a {@code Map<String, ?>}.
	 *
	 * @return True, if this relationship is dynamic
	 */
	boolean isDynamic();

	/**
	 * The source of this relationship is described by the primary label of the node in question.
	 *
	 * @return The source of this relationship
	 */
	NodeDescription<?> getSource();

	/**
	 * The target of this relationship is described by the primary label of the node in question.
	 *
	 * @return The target of this relationship
	 */
	NodeDescription<?> getTarget();

	/**
	 * The name of the property where the relationship was defined. This is used by the Cypher creation to name the
	 * return values.
	 *
	 * @return The name of the field storing the relationship property
	 */
	String getFieldName();

	/**
	 * The direction of the defined relationship. This is used by the Cypher creation to query for relationships
	 * and create them with the right directions.
	 *
	 * @return The direction of the relationship
	 */
	Direction getDirection();

	default boolean isOutgoing() {
		return Direction.OUTGOING.equals(this.getDirection());
	}

	default boolean isIncoming() {
		return Direction.INCOMING.equals(this.getDirection());
	}

	@NotNull
	default String generateRelatedNodesCollectionName() {

		return this.getSource().getPrimaryLabel() + "_" + this.getType() + "_" + this.getTarget().getPrimaryLabel();
	}
}
