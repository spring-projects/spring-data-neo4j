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
package org.springframework.data.neo4j.core;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.driver.Record;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.neo4j.core.context.DefaultPersistenceContext;
import org.springframework.data.neo4j.core.context.PersistenceContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL, since = "1.0")
class DefaultNodeManager implements NodeManager {

	private final Schema schema;

	private final Neo4jClient neo4jClient;

	private final Transaction transaction;

	private final PersistenceContext persistenceContext;

	DefaultNodeManager(Schema schema, Neo4jClient neo4jClient, @Nullable Transaction transaction) {

		this.schema = schema;
		this.neo4jClient = neo4jClient;
		this.transaction = transaction;

		this.persistenceContext = new DefaultPersistenceContext(schema);
	}

	@Override
	@Nullable
	public Transaction getTransaction() {
		return transaction;
	}

	@Override
	public Object executeQuery(String query) {

		return neo4jClient.newQuery(query).fetch().one();
	}

	@Override
	public <T> Optional<T> executeTypedQueryForObject(String query, Class<T> returnedType) {
		Optional<NodeDescription<?>> nodeDescriptionOptional = schema.getNodeDescription(returnedType);
		// returned type can be sth. like a raw result or scalar
		if (!nodeDescriptionOptional.isPresent()) {
			return getTypedMappingSpec(query, returnedType).one();
		}

		return getTypedFetchSpec(query, returnedType, (NodeDescription<T>) nodeDescriptionOptional.get()).one();
	}

	@Override
	public <T> Collection<T> executeTypedQueryForObjects(String query, Class<T> returnedType) {

		Optional<NodeDescription<?>> nodeDescriptionOptional = schema.getNodeDescription(returnedType);
		// returned type can be sth. like a raw result or scalar
		if (!nodeDescriptionOptional.isPresent()) {
			return getTypedMappingSpec(query, returnedType).all();
		}

		return getTypedFetchSpec(query, returnedType, (NodeDescription<T>) nodeDescriptionOptional.get()).all();
	}

	private <T> Neo4jClient.RecordFetchSpec<Optional<T>, Collection<T>, T> getTypedFetchSpec(String query,
			Class<T> returnedType, NodeDescription<T> nodeDescription) {

		Function<Record, T> mappingFunction = new ReflectionBasedMappingFunction<T>(nodeDescription);

		return getTypedMappingSpec(query, returnedType).mappedBy(mappingFunction);
	}

	private <T> Neo4jClient.MappingSpec<Optional<T>, Collection<T>, T> getTypedMappingSpec(String query,
			Class<T> returnedType) {

		return neo4jClient.newQuery(query).fetchAs(returnedType);
	}

	@Override
	public <T> T save(T entityWithUnknownState) {

		// TODO if already registered, here or in the context?
		this.persistenceContext.register(entityWithUnknownState);

		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	public void delete(Object managedEntity) {

		this.persistenceContext.deregister(managedEntity);

		throw new UnsupportedOperationException("Not there yet.");
	}

	private class ReflectionBasedMappingFunction<T> implements Function<Record, T> {

		private final NodeDescription<T> nodeDescription;

		private ReflectionBasedMappingFunction(NodeDescription<T> nodeDescription) {
			this.nodeDescription = nodeDescription;
		}

		@Override
		public T apply(Record record) {

			try {

				for (Value value : record.values()) {
					// todo
					// 1. determine type of returned value: node or relationship
					// 2. find most fitting label value.asNode().labels().forEach();
					// strictly assume just to work with nodes for now and every matching type label is the root node
					if (value.asNode().hasLabel(nodeDescription.getPrimaryLabel())) {

						if (!(nodeDescription instanceof Neo4jPersistentEntity)) {
							return null;
						}

						Neo4jPersistentEntity<T> entity = (Neo4jPersistentEntity<T>) nodeDescription;
						PreferredConstructor<T, Neo4jPersistentProperty> persistenceConstructor = entity
								.getPersistenceConstructor();

						// todo check because there was "something" wrong with the usage of EntityInstantiators in SDN
						EntityInstantiators instantiators = new EntityInstantiators();

						// todo: neo4jPersistentEntity.requiresPropertyPopulation()

						T instance = instantiators.getInstantiatorFor(entity).createInstance(entity,
								new ParameterValueProvider<Neo4jPersistentProperty>() {
									@Override
									public Object getParameterValue(PreferredConstructor.Parameter parameter) {
										return value.get(parameter.getName()).asObject();
									}
								});

						PersistentPropertyAccessor<T> propertyAccessor = entity.getPropertyAccessor(instance);
						if (entity.requiresPropertyPopulation()) {
							entity.doWithProperties((SimplePropertyHandler) property -> {
								if (persistenceConstructor.isConstructorParameter(property)) {
									return;
								}

								propertyAccessor.setProperty(property, value.get(property.getName()).asObject());

							});
							instance = propertyAccessor.getBean();
						}

						return instance;
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

	}
}
