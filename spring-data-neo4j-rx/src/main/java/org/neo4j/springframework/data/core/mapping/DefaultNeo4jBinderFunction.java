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
package org.neo4j.springframework.data.core.mapping;

import static org.neo4j.springframework.data.core.schema.NodeDescription.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.neo4j.springframework.data.core.convert.Neo4jConverter;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * @author Michael J. Simons
 * @param <T> type that should get mapped by the binder function
 * @since 1.0
 */
final class DefaultNeo4jBinderFunction<T> implements Function<T, Map<String, Object>> {

	private final Neo4jPersistentEntity<T> nodeDescription;

	private final Neo4jConverter converter;

	DefaultNeo4jBinderFunction(Neo4jPersistentEntity<T> nodeDescription, Neo4jConverter converter) {
		this.nodeDescription = nodeDescription;
		this.converter = converter;
	}

	@Override
	public Map<String, Object> apply(T entity) {
		Map<String, Object> properties = new HashMap<>();

		PersistentPropertyAccessor<T> propertyAccessor = nodeDescription.getPropertyAccessor(entity);
		nodeDescription.doWithProperties((Neo4jPersistentProperty p) -> {

			// Skip the internal properties, we don't want them to end up stored as properties
			if (p.isInternalIdProperty()) {
				return;
			}

			final Object value = converter.writeValue(propertyAccessor.getProperty(p), p.getTypeInformation());
			properties.put(p.getPropertyName(), value);
		});

		Map<String, Object> parameters = new HashMap<>();
		parameters.put(NAME_OF_PROPERTIES_PARAM, properties);
		parameters.put(NAME_OF_ID_PARAM, propertyAccessor.getProperty(nodeDescription.getRequiredIdProperty()));

		return parameters;
	}
}
