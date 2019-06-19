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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.neo4j.springframework.data.core.schema.SchemaUtils;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.lang.Nullable;

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

	private final Neo4jMappingContext mappingContext;

	private static final Predicate<Map.Entry<String, Object>> IS_LIST = entry -> entry.getValue() instanceof List;

	DefaultNeo4jMappingFunction(Neo4jPersistentEntity<T> rootNodeDescription, Neo4jMappingContext neo4jMappingContext) {

		this.rootNodeDescription = rootNodeDescription;
		this.mappingContext = neo4jMappingContext;
	}

	@Override
	public T apply(TypeSystem typeSystem, Record record) {
		Map<Object, Object> knownObjects = new ConcurrentHashMap<>();
		try {
			Predicate<Value> isNode = v -> v.hasType(typeSystem.NODE());
			Predicate<Value> isMap = value -> value.hasType(typeSystem.MAP());

			List<Value> recordValues = record.values();

			String nodeLabel = rootNodeDescription.getPrimaryLabel();
			List<Node> nodes = recordValues.stream()
				.filter(isNode)
				.map(Value::asNode)
				.filter(node -> node.hasLabel(nodeLabel))
				.collect(toList());

			// either the results will be node based
			if (!nodes.isEmpty()) {
				// todo find most fitting label value.asNode().labels().forEach();
				return nodes.stream().filter(node -> node.hasLabel(nodeLabel)).findFirst()
					.map(node -> mergeIntoMap(node, record))
					.map(mergedAttributes -> map(mergedAttributes, rootNodeDescription, knownObjects))
					.orElseGet(() -> {
						log.warn(() -> String.format("Could not find mappable nodes or relationships inside %s for %s", record, rootNodeDescription));
						return null;
					});
			} else { // or it is a mostly generated result that is represented as a map
				return recordValues.stream().filter(isMap)
					.map(map -> map(map.asMap(), rootNodeDescription, knownObjects)).findFirst().get();
		}
		} catch (Exception e) {
			throw new MappingException("Error mapping " + record.toString(), e);
		}
	}

	/**
	 * Merges the root node of a query and the remaining record into one map, adding the internal ID of the node, too.
	 *
	 * @param node   Node whose attributes are about to be merged
	 * @param record Optional record that should be merged
	 * @return
	 */
	private static Map<String, Object> mergeIntoMap(Node node, @Nullable Record record) {
		Map<String, Object> mergedAttributes = new HashMap<>(node.asMap());
		mergedAttributes.put(NAME_OF_INTERNAL_ID, node.id());
		if (record != null) {
			mergedAttributes.putAll(record.asMap());
		}
		return mergedAttributes;
	}

	/**
	 * @param queryResult     The original query result
	 * @param nodeDescription The node description of the current entity to be mapped from the result
	 * @param knownObjects    The current list of known objects
	 * @param <ET>            As in entity type
	 * @return
	 */
	private <ET> ET map(Map<String, Object> queryResult, Neo4jPersistentEntity<ET> nodeDescription,
		Map<Object, Object> knownObjects) {

		ET instance = instantiate(nodeDescription, queryResult);

		PersistentPropertyAccessor<ET> propertyAccessor = nodeDescription.getPropertyAccessor(instance);
		if (nodeDescription.requiresPropertyPopulation()) {

			// Fill simple properties
			Predicate<Neo4jPersistentProperty> isConstructorParameter = nodeDescription
				.getPersistenceConstructor()::isConstructorParameter;
			nodeDescription.doWithProperties(populateFrom(queryResult, propertyAccessor, isConstructorParameter));

			// Fill associations
			Collection<RelationshipDescription> relationships = mappingContext
				.getRelationshipsOf(nodeDescription.getPrimaryLabel());
			Function<String, Neo4jPersistentEntity<?>> relatedNodeDescriptionLookup =
				relatedLabel -> (Neo4jPersistentEntity<?>) mappingContext.getNodeDescription(relatedLabel);
			nodeDescription.doWithAssociations(
				populateFrom(queryResult, propertyAccessor, relationships, relatedNodeDescriptionLookup, knownObjects));
		}
		return instance;
	}

	private static <ET> ET instantiate(Neo4jPersistentEntity<ET> anotherNodeDescription, Map<String, Object> values) {
		return INSTANTIATORS.getInstantiatorFor(anotherNodeDescription).createInstance(anotherNodeDescription,
			new ParameterValueProvider<Neo4jPersistentProperty>() {
				@Override
				public Object getParameterValue(PreferredConstructor.Parameter parameter) {

					Neo4jPersistentProperty matchingProperty = anotherNodeDescription
						.getRequiredPersistentProperty(parameter.getName());
					return extractValueOf(matchingProperty, values);
				}
			});
	}

	private static PropertyHandler<Neo4jPersistentProperty> populateFrom(
		Map<String, Object> queryResult,
		PersistentPropertyAccessor<?> propertyAccessor,
		Predicate<Neo4jPersistentProperty> isConstructorParameter
	) {
		return property -> {
			if (isConstructorParameter.test(property)) {
				return;
			}

			Object value = extractValueOf(property, queryResult);
			propertyAccessor.setProperty(property, value);
		};
	}

	private AssociationHandler<Neo4jPersistentProperty> populateFrom(
		Map<String, Object> queryResult,
		PersistentPropertyAccessor<?> propertyAccessor,
		Collection<RelationshipDescription> relationships,
		Function<String, Neo4jPersistentEntity<?>> relatedNodeDescriptionLookup,
		Map<Object, Object> knownObjects
	) {
		return association -> {
			Neo4jPersistentProperty inverse = association.getInverse();

			RelationshipDescription relationship = relationships.stream()
				.filter(r -> r.getPropertyName().equals(inverse.getName()))
				.findFirst().get();

			String relationshipType = relationship.getType();
			String targetLabel = relationship.getTarget();

			Neo4jPersistentEntity<?> targetNodeDescription = relatedNodeDescriptionLookup.apply(targetLabel);

			List<Object> value = new ArrayList<>();
			List list = (List) queryResult.get(SchemaUtils.generateRelatedNodesCollectionName(relationship));

			// if the list is null the mapping is based on a custom query
			if (list == null) {

				Predicate<Map.Entry<String, Object>> containsOnlyRelationships = entry -> ((List) entry.getValue())
					.stream()
					.allMatch(listEntry -> {
						if (!(listEntry instanceof Relationship)) {
							return false;
						}

						return ((Relationship) listEntry).type().equals(relationshipType);
					});

				Predicate<Map.Entry<String, Object>> containsOnlyNodes = entry -> ((List) entry.getValue()).stream()
					.allMatch(listEntry -> {

						if (!(listEntry instanceof Node)) {
							return false;
						}

						List<String> labels = new ArrayList<>();
						(((Node) listEntry).labels()).forEach(labels::add);
						return labels.contains(targetLabel);
					});

				// find relationships in the result
				// this is a List<List<Relationship>>
				List<Object> allMatchingTypeRelationshipsInResult = queryResult.entrySet().stream()
					.filter(IS_LIST.and(containsOnlyRelationships))
					.map(Map.Entry::getValue)
					.collect(toList());

				// this is a List<List<Node>>
				List<Object> allNodesWithMatchingLabelInResult = queryResult.entrySet().stream()
					.filter(IS_LIST.and(containsOnlyNodes))
					.map(Map.Entry::getValue)
					.collect(toList());

				if (allNodesWithMatchingLabelInResult.isEmpty() && allMatchingTypeRelationshipsInResult.isEmpty()) {
					return;
				}

				for (Object nodeWithMatchingLabel : allNodesWithMatchingLabelInResult) {
					for (Node possibleValueNode : (List<Node>) nodeWithMatchingLabel) {
						long nodeId = possibleValueNode.id();

						for (Object relList : allMatchingTypeRelationshipsInResult) {
							for (Relationship possibleRelationship : (List<Relationship>) relList) {
								if (possibleRelationship.endNodeId() == nodeId) {
									Map<String, Object> newPropertyMap = mergeIntoMap(possibleValueNode, null);
									value.add(map(newPropertyMap, targetNodeDescription, knownObjects));
									break;
								}
							}
						}
					}
				}
			} else {
				for (Object relatedEntity : list) {
					Map<String, Object> relatedEntityValues = (Map<String, Object>) relatedEntity;
					Neo4jPersistentProperty idProperty = targetNodeDescription.getRequiredIdProperty();

					// internal (generated) id or external set
					Object idValue = idProperty.isInternalIdProperty()
						? relatedEntityValues.get(NAME_OF_INTERNAL_ID)
						: relatedEntityValues.get(idProperty.getName());

					Object valueEntry = knownObjects.computeIfAbsent(idValue,
						(id) -> map(relatedEntityValues, targetNodeDescription, knownObjects));

					value.add(valueEntry);
				}
			}

			if (inverse.getTypeInformation().isCollectionLike()) {
				if (inverse.getType().equals(Set.class)) {
					propertyAccessor.setProperty(inverse, new HashSet(value));
				} else {
					propertyAccessor.setProperty(inverse, value);
				}
			} else {
				propertyAccessor.setProperty(inverse, value.isEmpty() ? null : value.get(0));
			}
		};
	}

	private static Object extractValueOf(Neo4jPersistentProperty property, Map<String, Object> propertyContainer) {
		if (property.isInternalIdProperty()) {
			return propertyContainer.get(NAME_OF_INTERNAL_ID);
		} else {
			String graphPropertyName = property.getPropertyName();
			return getValueFor(graphPropertyName, propertyContainer);
		}
	}

	private static Object getValueFor(String graphProperty, Map<String, Object> entity) {

		// TODO conversion, Type system
		return entity.get(graphProperty);
	}
}
