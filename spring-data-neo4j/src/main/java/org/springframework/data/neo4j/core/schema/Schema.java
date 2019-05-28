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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;

/**
 * Contains the descriptions of all nodes, their properties and relationships known to SDN-RX.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Schema {

	/**
	 * Registers  the given set of classes to be available as Neo4j domain entities.
	 *
	 * @param initialEntitySet The set of classes to register with this schema
	 */
	void setInitialEntitySet(Set<? extends Class<?>> initialEntitySet);

	/**
	 * Triggers the scanning of the registered, initial entity set.
	 */
	void initialize();

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
	 * @param underlyingClass
	 * @return The node description for the given. class
	 * @throws UnknownEntityException When {@code targetClass} is not a known entity class.
	 */
	default NodeDescription<?> getRequiredNodeDescription(Class<?> underlyingClass) {

		return getNodeDescription(underlyingClass).orElseThrow(() -> new UnknownEntityException(underlyingClass));
	}

	/**
	 * This returns the outgoing relationships this node has to other nodes.
	 *
	 * @param primaryLabel The primary label of the node whos relationships should be retrieved
	 * @return The relationships defined by instances of this node.
	 */
	Collection<RelationshipDescription> getRelationshipsOf(String primaryLabel);

	/**
	 * Retrieves a schema based mapping function for the {@code targetClass}. The mapping function will expect a
	 * record containing all the nodes and relationships necessary to fully populate an instance of the given class.
	 * It will not try to fetch data from any other records or queries. The mapping function is free to throw a {@link RuntimeException},
	 * most likely a {@code org.springframework.data.mapping.MappingException} or {@link IllegalStateException} when
	 * mapping is not possible.
	 * <p/>
	 * In case the mapping function returns a {@literal null}, the Neo4j client will throw an exception and prevent further
	 * processing.
	 *
	 * @param targetClass The target class to which to map to.
	 * @param <T>         Type of the target class
	 * @return An empty optional if the target class is unknown, otherwise an optional containing a stateless, reusable mapping function
	 */
	<T> Optional<BiFunction<TypeSystem, Record, T>> getMappingFunctionFor(Class<T> targetClass);

	/**
	 * @param targetClass The target class to which to map to.
	 * @param <T>         Type of the target class
	 * @return The default mapping function for the given target class
	 * @throws IllegalStateException When {@code targetClass} is not a managed class
	 * @see #getMappingFunctionFor(Class)
	 */
	default <T> BiFunction<TypeSystem, Record, T> getRequiredMappingFunctionFor(Class<T> targetClass) {

		return getMappingFunctionFor(targetClass).orElseThrow(() -> new UnknownEntityException(targetClass));
	}

	<T> Optional<Function<T, Map<String, Object>>> getBinderFunctionFor(Class<T> sourceClass);

	default <T> Function<T, Map<String, Object>> getRequiredBinderFunctionFor(Class<T> sourceClass) {

		return getBinderFunctionFor(sourceClass).orElseThrow(() -> new UnknownEntityException(sourceClass));
	}
}
