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

import java.util.Collection;
import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.Expression;
import org.springframework.lang.Nullable;

/**
 * Describes how a class is mapped to a node inside the database. It provides navigable links to relationships and
 * access to the nodes properties.
 *
 * @param <T> The type of the underlying class
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface NodeDescription<T> {

	String NAME_OF_ROOT_NODE = "n";
	String NAME_OF_INTERNAL_ID = "__internalNeo4jId__";
	String NAME_OF_IDS_RESULT = "__ids__";
	String NAME_OF_ID_PARAM = "__id__";
	String NAME_OF_PROPERTIES_PARAM = "__properties__";
	String NAME_OF_ENTITY_LIST_PARAM = "__entities__";

	/**
	 * @return The primary label of this entity inside Neo4j.
	 */
	String getPrimaryLabel();

	/**
	 * @return The concrete class to which a node with the given {@link #getPrimaryLabel()} is mapped to
	 */
	Class<T> getUnderlyingClass();

	/**
	 * @return A description how to determine primary ids for nodes fitting this description
	 */
	@Nullable
	IdDescription getIdDescription();

	/**
	 * @return A collection of persistent properties that are mapped to graph properties and not to relationships
	 */
	Collection<GraphPropertyDescription> getGraphProperties();

	/**
	 * Retrieves a {@link GraphPropertyDescription} by its field name.
	 *
	 * @param fieldName The field name for which the graph property description should be retrieved
	 * @return An empty optional if there is no property known for the given field.
	 */
	Optional<GraphPropertyDescription> getGraphProperty(String fieldName);

	/**
	 * @return True if entities for this node use Neo4j internal ids.
	 */
	default boolean isUsingInternalIds() {
		return this.getIdDescription().isInternallyGeneratedId();
	}

	/**
	 * This returns the outgoing relationships this node has to other nodes.
	 *
	 * @return The relationships defined by instances of this node.
	 */
	Collection<RelationshipDescription> getRelationships();

	/**
	 * @return An expression that represents the right identifier type.
	 */
	default Expression getIdExpression() {

		return this.getIdDescription().asIdExpression();
	}
}
