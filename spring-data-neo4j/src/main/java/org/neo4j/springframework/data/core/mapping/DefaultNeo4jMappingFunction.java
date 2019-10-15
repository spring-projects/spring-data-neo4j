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
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;
import static org.neo4j.springframework.data.core.schema.RelationshipDescription.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.convert.Neo4jConverter;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
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

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(DefaultNeo4jMappingFunction.class));

	/**
	 * The shared entity instantiators of this context. Those should not be recreated for each entity or even not for
	 * each query, as otherwise the cache of Spring's org.springframework.data.convert.ClassGeneratingEntityInstantiator
	 * won't apply
	 */
	private static final EntityInstantiators INSTANTIATORS = new EntityInstantiators();

	/**
	 * The description of the possible root node from where the mapping should start.
	 */
	private final Neo4jPersistentEntity<T> rootNodeDescription;

	private final Neo4jConverter converter;

	DefaultNeo4jMappingFunction(Neo4jPersistentEntity<T> rootNodeDescription, Neo4jConverter converter) {

		this.rootNodeDescription = rootNodeDescription;
		this.converter = converter;
	}

	@Override
	public T apply(TypeSystem typeSystem, Record record) {
		// That would be the place to call a custom converter for the whole object, if any such thing would be
		// available (Converter<Record, DomainObject>
		try {
			List<Value> recordValues = record.values();
			String nodeLabel = rootNodeDescription.getPrimaryLabel();
			MapAccessor queryRoot = null;
			for (Value value : recordValues) {
				if (value.hasType(typeSystem.NODE()) && value.asNode().hasLabel(nodeLabel)) {
					if (recordValues.size() > 1) {
						queryRoot = mergeRootNodeWithRecord(value.asNode(), record);
					} else {
						queryRoot = value.asNode();
					}
					break;
				}
			}
			if (queryRoot == null) {
				for (Value value : recordValues) {
					if (value.hasType(typeSystem.MAP())) {
						queryRoot = value;
						break;
					}
				}
			}

			if (queryRoot == null) {
				log.warn(() -> String.format("Could not find mappable nodes or relationships inside %s for %s", record,
					rootNodeDescription));
				return null;
			} else {
				Map<Object, Object> knownObjects = new ConcurrentHashMap<>();
				return map(typeSystem, queryRoot, rootNodeDescription, knownObjects);
			}
		} catch (Exception e) {
			throw new MappingException("Error mapping " + record.toString(), e);
		}
	}

	/**
	 * Merges the root node of a query and the remaining record into one map, adding the internal ID of the node, too.
	 * Merge happens only when the record contains additional values.
	 *
	 * @param node   Node whose attributes are about to be merged
	 * @param record Record that should be merged
	 * @return
	 */
	private static MapAccessor mergeRootNodeWithRecord(Node node, Record record) {
		Map<String, Object> mergedAttributes = new HashMap<>(node.size() + record.size() + 1);

		mergedAttributes.put(NAME_OF_INTERNAL_ID, node.id());
		mergedAttributes.putAll(node.asMap(Function.identity()));
		mergedAttributes.putAll(record.asMap(Function.identity()));

		return Values.value(mergedAttributes);
	}

	/**
	 * @param queryResult     The original query result
	 * @param nodeDescription The node description of the current entity to be mapped from the result
	 * @param knownObjects    The current list of known objects
	 * @param <ET>            As in entity type
	 * @return
	 */
	private <ET> ET map(TypeSystem typeSystem, MapAccessor queryResult,
		Neo4jPersistentEntity<ET> nodeDescription,
		Map<Object, Object> knownObjects) {

		ET instance = instantiate(nodeDescription, queryResult);

		PersistentPropertyAccessor<ET> propertyAccessor = converter
			.decoratePropertyAccessor(typeSystem, nodeDescription.getPropertyAccessor(instance));
		if (nodeDescription.requiresPropertyPopulation()) {

			// Fill simple properties
			Predicate<Neo4jPersistentProperty> isConstructorParameter = nodeDescription
				.getPersistenceConstructor()::isConstructorParameter;
			nodeDescription.doWithProperties(populateFrom(queryResult, propertyAccessor, isConstructorParameter));

			// Fill associations
			Collection<RelationshipDescription> relationships = nodeDescription.getRelationships();
			nodeDescription.doWithAssociations(
				populateFrom(typeSystem, queryResult, propertyAccessor, relationships, knownObjects));
		}
		return instance;
	}

	private <ET> ET instantiate(Neo4jPersistentEntity<ET> anotherNodeDescription, MapAccessor values) {

		ParameterValueProvider<Neo4jPersistentProperty> parameterValueProvider = new ParameterValueProvider<Neo4jPersistentProperty>() {
			@Override
			public Value getParameterValue(PreferredConstructor.Parameter parameter) {

				Neo4jPersistentProperty matchingProperty = anotherNodeDescription
					.getRequiredPersistentProperty(parameter.getName());
				return extractValueOf(matchingProperty, values);
			}
		};
		parameterValueProvider = converter.decorateParameterValueProvider(parameterValueProvider);
		return INSTANTIATORS.getInstantiatorFor(anotherNodeDescription)
			.createInstance(anotherNodeDescription, parameterValueProvider);
	}

	private static PropertyHandler<Neo4jPersistentProperty> populateFrom(
		MapAccessor queryResult,
		PersistentPropertyAccessor<?> propertyAccessor,
		Predicate<Neo4jPersistentProperty> isConstructorParameter
	) {
		return property -> {
			if (isConstructorParameter.test(property)) {
				return;
			}

			Value value = extractValueOf(property, queryResult);
			propertyAccessor.setProperty(property, value);
		};
	}

	private AssociationHandler<Neo4jPersistentProperty> populateFrom(
		TypeSystem typeSystem,
		MapAccessor queryResult,
		PersistentPropertyAccessor<?> propertyAccessor,
		Collection<RelationshipDescription> relationships,
		Map<Object, Object> knownObjects
	) {
		return association -> {
			Neo4jPersistentProperty inverse = association.getInverse();

			RelationshipDescription relationship = relationships.stream()
				.filter(r -> r.getFieldName().equals(inverse.getName()))
				.findFirst().get();

			String relationshipType = relationship.getType();
			String targetLabel = relationship.getTarget().getPrimaryLabel();

			Neo4jPersistentEntity<?> targetNodeDescription = (Neo4jPersistentEntity<?>) relationship.getTarget();

			List<Object> value = new ArrayList<>();
			Map<String, Object> dynamicValue = new HashMap<>();

			BiConsumer<String, Object> mappedObjectHandler = relationship.isDynamic() ?
				dynamicValue::put : (type, mappedObject) -> value.add(mappedObject);

			Value list = queryResult.get(relationship.generateRelatedNodesCollectionName());

			// if the list is null the mapping is based on a custom query
			if (list == Values.NULL) {

				Predicate<Value> isList = entry -> entry instanceof Value && typeSystem.LIST().isTypeOf(entry);

				Predicate<Value> containsOnlyRelationships = entry -> entry.asList(Function.identity())
					.stream()
					.allMatch(listEntry -> typeSystem.RELATIONSHIP().isTypeOf(listEntry));

				Predicate<Value> containsOnlyNodes = entry -> entry.asList(Function.identity())
					.stream()
					.allMatch(listEntry -> typeSystem.NODE().isTypeOf(listEntry));

				// find relationships in the result
				List<Relationship> allMatchingTypeRelationshipsInResult = StreamSupport
					.stream(queryResult.values().spliterator(), false)
					.filter(isList.and(containsOnlyRelationships))
					.flatMap(entry -> entry.asList(Value::asRelationship).stream())
					.filter(r -> r.type().equals(relationshipType))
					.collect(toList());

				List<Node> allNodesWithMatchingLabelInResult = StreamSupport
					.stream(queryResult.values().spliterator(), false)
					.filter(isList.and(containsOnlyNodes))
					.flatMap(entry -> entry.asList(Value::asNode).stream())
					.filter(n -> n.hasLabel(targetLabel))
					.collect(toList());

				if (allNodesWithMatchingLabelInResult.isEmpty() && allMatchingTypeRelationshipsInResult.isEmpty()) {
					return;
				}

				for (Node possibleValueNode : allNodesWithMatchingLabelInResult) {
						long nodeId = possibleValueNode.id();

					for (Relationship possibleRelationship : allMatchingTypeRelationshipsInResult) {
						if (possibleRelationship.endNodeId() == nodeId) {
							Object mappedObject = map(typeSystem, possibleValueNode, targetNodeDescription, knownObjects);
							mappedObjectHandler.accept(possibleRelationship.type(), mappedObject);
							break;
						}
					}
				}
			} else {
				for (Value relatedEntity : list.asList(Function.identity())) {
					Neo4jPersistentProperty idProperty = targetNodeDescription.getRequiredIdProperty();

					// internal (generated) id or external set
					Object idValue = idProperty.isInternalIdProperty()
						? relatedEntity.get(NAME_OF_INTERNAL_ID)
						: relatedEntity.get(idProperty.getName());
					Object valueEntry = knownObjects.computeIfAbsent(idValue,
						(id) -> map(typeSystem, relatedEntity, targetNodeDescription, knownObjects));

					mappedObjectHandler.accept(relatedEntity.get(NAME_OF_RELATIONSHIP_TYPE).asString(), valueEntry);
				}
			}

			if (inverse.getTypeInformation().isCollectionLike()) {
				if (inverse.getType().equals(Set.class)) {
					propertyAccessor.setProperty(inverse, new HashSet(value));
				} else {
					propertyAccessor.setProperty(inverse, value);
				}
			} else {
				if (relationship.isDynamic()) {
					propertyAccessor.setProperty(inverse, dynamicValue.isEmpty() ? null : dynamicValue);
				} else {
					propertyAccessor.setProperty(inverse, value.isEmpty() ? null : value.get(0));
				}
			}
		};
	}

	private static Value extractValueOf(Neo4jPersistentProperty property, MapAccessor propertyContainer) {
		if (property.isInternalIdProperty()) {
			return propertyContainer instanceof Node ?
				Values.value(((Node) propertyContainer).id()) :
				propertyContainer.get(NAME_OF_INTERNAL_ID);
		} else {
			String graphPropertyName = property.getPropertyName();
			return propertyContainer.get(graphPropertyName);
		}
	}
}
