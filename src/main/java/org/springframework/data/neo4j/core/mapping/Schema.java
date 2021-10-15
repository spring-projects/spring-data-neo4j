/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.lang.Nullable;

/**
 * Contains the descriptions of all nodes, their properties and relationships known to SDN.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public interface Schema {

	/**
	 * Retrieves a node's description by its primary label.
	 *
	 * @param primaryLabel The primary label under which the node is described
	 * @return The description if any, null otherwise
	 */
	@Nullable
	NodeDescription<?> getNodeDescription(String primaryLabel);

	/**
	 * Retrieves a node's description by its underlying class.
	 *
	 * @param underlyingClass The underlying class of the node description to be retrieved
	 * @return The description if any, null otherwise
	 */
	@Nullable
	NodeDescription<?> getNodeDescription(Class<?> underlyingClass);

	default NodeDescription<?> getRequiredNodeDescription(Class<?> underlyingClass) {
		NodeDescription<?> nodeDescription = getNodeDescription(underlyingClass);
		if (nodeDescription == null) {
			throw new UnknownEntityException(underlyingClass);
		}
		return nodeDescription;
	}

	default NodeDescription<?> getRequiredNodeDescription(String primaryLabel) {
		NodeDescription<?> nodeDescription = getNodeDescription(primaryLabel);
		if (nodeDescription == null) {
			throw new MappingException(
					String.format("Required node description not found with primary label '%s'", primaryLabel));
		}
		return nodeDescription;
	}

	/**
	 * Retrieves a schema based mapping function for the {@code targetClass}. The mapping function will expect a record
	 * containing all the nodes and relationships necessary to fully populate an instance of the given class. It will not
	 * try to fetch data from any other records or queries. The mapping function is free to throw a
	 * {@link RuntimeException}, most likely a {@code org.springframework.data.mapping.MappingException} or
	 * {@link IllegalStateException} when mapping is not possible.
	 * <p>
	 * In case the mapping function returns a {@literal null}, the Neo4j client will throw an exception and prevent
	 * further processing.
	 *
	 * @param targetClass The target class to which to map to.
	 * @param <T> Type of the target class
	 * @return The default, stateless and reusable mapping function for the given target class
	 * @throws UnknownEntityException When {@code targetClass} is not a managed class
	 */
	default <T> BiFunction<TypeSystem, MapAccessor, T> getRequiredMappingFunctionFor(Class<T> targetClass) {
		NodeDescription<?> nodeDescription = getNodeDescription(targetClass);
		if (nodeDescription == null) {
			throw new UnknownEntityException(targetClass);
		}
		Neo4jEntityConverter entityConverter = getEntityConverter();
		return (typeSystem, record) -> entityConverter.read(targetClass, record);
	}

	/**
	 * @return The (reading and writing) converter used to read records into entities and write entities into maps.
	 */
	Neo4jEntityConverter getEntityConverter();

	default <T> Function<T, Map<String, Object>> getRequiredBinderFunctionFor(Class<T> sourceClass) {

		if (getNodeDescription(sourceClass) == null) {
			throw new UnknownEntityException(sourceClass);
		}

		Neo4jEntityConverter entityConverter = getEntityConverter();
		return t -> {
			Map<String, Object> parameters = new HashMap<>();
			entityConverter.write(t, parameters);
			return parameters;
		};
	}

	/**
	 * Creates or retrieves an instance of the given id generator class. During the lifetime of the schema, this method
	 * returns the same instance of reoccurring requests of the same type.
	 *
	 * @param idGeneratorType The type of the ID generator to return
	 * @return The id generator.
	 */
	<T extends IdGenerator<?>> T getOrCreateIdGeneratorOfType(Class<T> idGeneratorType);

	<T extends IdGenerator<?>> Optional<T> getIdGenerator(String reference);
}
