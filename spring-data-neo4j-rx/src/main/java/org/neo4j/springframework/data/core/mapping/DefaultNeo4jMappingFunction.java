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

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.ParameterValueProvider;

/**
 * The central logic of mapping Neo4j's {@link org.neo4j.driver.Record records} to entities based on the Spring
 * implementation of the {@link org.neo4j.springframework.data.core.schema.Schema},
 * represented by the {@link Neo4jMappingContext}.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
final class DefaultNeo4jMappingFunction<T> implements BiFunction<TypeSystem, Record, T> {

	private static final Logger log = LoggerFactory.getLogger(DefaultNeo4jMappingFunction.class);

	private final EntityInstantiators instantiators;

	private final Neo4jPersistentEntity<T> nodeDescription;

	DefaultNeo4jMappingFunction(EntityInstantiators instantiators, Neo4jPersistentEntity<T> nodeDescription) {
		this.instantiators = instantiators;
		this.nodeDescription = nodeDescription;
	}

	@Override
	public T apply(TypeSystem typeSystem, Record record) {

		try {
			Predicate<Value> isNode = v -> v.hasType(typeSystem.NODE());
			List<Node> nodes = record.values().stream().filter(isNode).map(Value::asNode).collect(toList());

			// todo
			// 1. determine type of returned value: node or relationship
			// 2. find most fitting label value.asNode().labels().forEach();
			// For now strictly assume just to work with nodes for now and every matching type label is the root node
			Optional<Node> optionalRootNode = nodes.stream()
				.filter(node -> node.hasLabel(nodeDescription.getPrimaryLabel())).findFirst();

			return optionalRootNode.map(rootNode -> {
				PreferredConstructor<T, Neo4jPersistentProperty> persistenceConstructor = nodeDescription
					.getPersistenceConstructor();

				// todo: neo4jPersistentEntity.requiresPropertyPopulation()

				T instance = instantiators.getInstantiatorFor(nodeDescription).createInstance(nodeDescription,
					new ParameterValueProvider<Neo4jPersistentProperty>() {
						@Override
						public Object getParameterValue(PreferredConstructor.Parameter parameter) {

							Neo4jPersistentProperty matchingProperty = nodeDescription
								.getRequiredPersistentProperty(parameter.getName());
							return extractValueOf(matchingProperty, record, rootNode);
						}
					});

				PersistentPropertyAccessor<T> propertyAccessor = nodeDescription.getPropertyAccessor(instance);
				if (nodeDescription.requiresPropertyPopulation()) {
					nodeDescription.doWithProperties((Neo4jPersistentProperty property) -> {
						if (persistenceConstructor.isConstructorParameter(property)) {
							return;
						}

						Object value = extractValueOf(property, record, rootNode);
						propertyAccessor.setProperty(property, value);
					});
					instance = propertyAccessor.getBean();
				}

				return instance;
			}).orElseGet(() -> {
				log.warn("Could not find mappable nodes or relationships inside {} for {}", record, nodeDescription);
				return null;
			});
		} catch (Exception e) {
			throw new MappingException("Error mapping " + record.toString(), e);
		}
	}

	Object extractValueOf(Neo4jPersistentProperty property, Record record, Entity propertyContainer) {
		if (property.isInternalIdProperty()) {
			return record.get(NodeDescription.NAME_OF_INTERNAL_ID, propertyContainer.id());
		} else {
			String graphPropertyName = property.getPropertyName();
			return getValueFor(graphPropertyName, propertyContainer);
		}
	}

	Object getValueFor(String graphProperty, Entity from) {

		// TODO conversion, Type system
		return from.get(graphProperty).asObject();
	}
}
