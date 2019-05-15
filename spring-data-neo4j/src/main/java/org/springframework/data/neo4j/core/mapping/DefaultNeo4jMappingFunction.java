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
package org.springframework.data.neo4j.core.mapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.neo4j.core.schema.GraphPropertyDescription;
import org.springframework.data.neo4j.core.schema.NodeDescription;

/**
 * The central logic of mapping Neo4j's {@link org.neo4j.driver.Record records} to entities based on the Spring
 * implementation of the {@link org.springframework.data.neo4j.core.schema.Schema},
 * represented by the {@link Neo4jMappingContext}.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@RequiredArgsConstructor
@Slf4j
class DefaultNeo4jMappingFunction<T> implements BiFunction<TypeSystem, Record, T> {

	private final EntityInstantiators instantiators;

	private final Neo4jPersistentEntity<T> nodeDescription;

	@Override
	public T apply(TypeSystem typeSystem, Record record) {

		try {

			for (Value value : record.values()) {
				// todo
				// 1. determine type of returned value: node or relationship
				// 2. find most fitting label value.asNode().labels().forEach();
				// strictly assume just to work with nodes for now and every matching type label is the root node
				if (value.asNode().hasLabel(nodeDescription.getPrimaryLabel())) {

					PreferredConstructor<T, Neo4jPersistentProperty> persistenceConstructor = nodeDescription
						.getPersistenceConstructor();

					// todo: neo4jPersistentEntity.requiresPropertyPopulation()

					T instance = instantiators.getInstantiatorFor(nodeDescription).createInstance(nodeDescription,
						new ParameterValueProvider<Neo4jPersistentProperty>() {
							@Override
							public Object getParameterValue(PreferredConstructor.Parameter parameter) {

								String graphPropertyName = getGraphPropertyNameFor(parameter);
								return getValueFor(graphPropertyName, value);
							}
						});

					PersistentPropertyAccessor<T> propertyAccessor = nodeDescription.getPropertyAccessor(instance);
					if (nodeDescription.requiresPropertyPopulation()) {
						nodeDescription.doWithProperties((Neo4jPersistentProperty property) -> {
							if (persistenceConstructor.isConstructorParameter(property)) {
								return;
							}

							String graphPropertyName = property.getPropertyName();
							propertyAccessor.setProperty(property, getValueFor(graphPropertyName, value));
						});
						instance = propertyAccessor.getBean();
					}

					return instance;
				}
			}
		} catch (Exception e) {
			throw new MappingException("Error mapping " + record.toString(), e);
		}

		log.warn("Could not find mappable nodes or relationships inside {} for {}", record, nodeDescription);
		return null;
	}

	/**
	 * Extracts the name of the graph property for a field and also for a constructor parameter. The {@link NodeDescription} should
	 * contain property descriptions for all fields, but may not contain a description for a constructor parameter if the parameter
	 * has a different name than the field. In this case the name itself is returned.
	 * <p/>
	 * Otherwise the name of the property in the graph will be returned.
	 *
	 * @param parameter The constructor parameter for which the property name is needed
	 * @return The name of the graph property or the name of {@code parameter} if a corresponding property was not found
	 */
	String getGraphPropertyNameFor(PreferredConstructor.Parameter parameter) {
		return nodeDescription
			.getGraphProperty(parameter.getName())
			.map(GraphPropertyDescription::getPropertyName)
			.orElse(parameter.getName());
	}

	Object getValueFor(String graphProperty, Value from) {

		// TODO conversion, Type system
		return from.get(graphProperty).asObject();
	}
}
