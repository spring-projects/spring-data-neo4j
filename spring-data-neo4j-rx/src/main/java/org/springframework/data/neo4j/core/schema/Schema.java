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
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;

/**
 * Contains the descriptions of all nodes, their properties and relationships known to SDN-RX.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Schema {

	/**
	 * Registers and scans the given set of classes to be available as Neo4j domain entities.
	 *
	 * @param entityClasses The additional set of classes to register with this schema
	 */
	void register(Set<? extends Class<?>> entityClasses);

	/**
	 * Retrieves a nodes description by its primary label.
	 *
	 * @param primaryLabel The primary label under which the node is described
	 * @return The description if any
	 */
	Optional<NodeDescription<?>> getNodeDescription(String primaryLabel);

	/**
	 * Retrieves a nodes description by its underlying class.
	 *
	 * @param underlyingClass The underlying class of the node description to be retrieved
	 * @return The description if any
	 */
	Optional<NodeDescription<?>> getNodeDescription(Class<?> underlyingClass);

	/**
	 * This returns the outgoing relationships this node has to other nodes.
	 *
	 * @param primaryLabel The primary label of the node whos relationships should be retrieved
	 * @return The relationships defined by instances of this node.
	 */
	Collection<RelationshipDescription> getRelationshipsOf(String primaryLabel);
}
