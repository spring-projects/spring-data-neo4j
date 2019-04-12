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
package org.springframework.data.neo4j.core.schema;

import java.util.Collection;

import org.apiguardian.api.API;

/**
 * Describes how a class is mapped to a node inside the database. It provides navigable links to relationships and
 * access to the nodes properties.
 *
 * @param <T> The type of the underlying class
 * @author Michael J. Simons
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface NodeDescription<T> {

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
	IdDescription getIdDescription();

	/**
	 * @return A collection of persistent properties that are mapped to graph properties and not to relationships
	 */
	Collection<GraphPropertyDescription> getGraphProperties();
}
