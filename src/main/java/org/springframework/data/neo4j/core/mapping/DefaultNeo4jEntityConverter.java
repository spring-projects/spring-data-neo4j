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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.Type;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.CollectionFactory;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @soundtrack The Kleptones - A Night At The Hip-Hopera
 * @since 6.0
 */
final class DefaultNeo4jEntityConverter implements Neo4jEntityConverter {

	private final EntityInstantiators entityInstantiators;
	private final NodeDescriptionStore nodeDescriptionStore;
	private final Neo4jConversionService conversionService;

	private final KnownObjects knownObjects = new KnownObjects();

	private final Type nodeType;
	private final Type relationshipType;
	private final Type mapType;
	private final Type listType;

	DefaultNeo4jEntityConverter(EntityInstantiators entityInstantiators, Neo4jConversionService conversionService,
			NodeDescriptionStore nodeDescriptionStore, TypeSystem typeSystem) {

		Assert.notNull(entityInstantiators, "EntityInstantiators must not be null!");
		Assert.notNull(conversionService, "Neo4jConversionService must not be null!");
		Assert.notNull(nodeDescriptionStore, "NodeDescriptionStore must not be null!");
		Assert.notNull(typeSystem, "TypeSystem must not be null!");

		this.entityInstantiators = entityInstantiators;
		this.conversionService = conversionService;
		this.nodeDescriptionStore = nodeDescriptionStore;

		this.nodeType = typeSystem.NODE();
		this.relationshipType = typeSystem.RELATIONSHIP();
		this.mapType = typeSystem.MAP();
		this.listType = typeSystem.LIST();
	}

	@Override
	public <R> R read(Class<R> targetType, MapAccessor mapAccessor) {

		Neo4jPersistentEntity<R> rootNodeDescription = (Neo4jPersistentEntity) nodeDescriptionStore.getNodeDescription(targetType);
		MapAccessor queryRoot = determineQueryRoot(mapAccessor, rootNodeDescription);
		if (queryRoot == null) {
			throw new NoRootNodeMappingException(String.format("Could not find mappable nodes or relationships inside %s for %s", mapAccessor, rootNodeDescription));
		}

		try {
			return map(queryRoot, queryRoot, rootNodeDescription, new HashSet<>());
		} catch (Exception e) {
			throw new MappingException("Error mapping " + mapAccessor.toString(), e);
		}
	}

	private <R> MapAccessor determineQueryRoot(MapAccessor mapAccessor, Neo4jPersistentEntity<R> rootNodeDescription) {

		String primaryLabel = rootNodeDescription.getPrimaryLabel();

		// Massage the initial mapAccessor into something we can deal with
		Iterable<Value> recordValues = mapAccessor instanceof Value && ((Value) mapAccessor).hasType(nodeType) ?
				Collections.singletonList((Value) mapAccessor) : mapAccessor.values();

		List<Node> matchingNodes = new ArrayList<>(); // The node that eventually becomes the query root. The list should only contain one node.
		List<Node> seenMatchingNodes = new ArrayList<>(); // A list of candidates: All things that are nodes and have a matching label

		for (Value value : recordValues) {
			if (value.hasType(nodeType)) { // It is a node
				Node node = value.asNode();
				if (node.hasLabel(primaryLabel)) { // it has a matching label
					// We haven't seen this node yet, so we take it
					if (knownObjects.getObject(node.id()) == null) {
						matchingNodes.add(node);
					} else {
						seenMatchingNodes.add(node);
					}
				}
			}
		}

		// Prefer the candidates over candidates previously seen
		List<Node> finalCandidates = matchingNodes.isEmpty() ? seenMatchingNodes : matchingNodes;
		MapAccessor queryRoot = null;

		if (finalCandidates.size() > 1) {
			throw new MappingException("More than one matching node in the record.");
		} else if (!finalCandidates.isEmpty()) {
			if (mapAccessor.size() > 1) {
				queryRoot = mergeRootNodeWithRecord(finalCandidates.get(0), mapAccessor);
			} else {
				queryRoot = finalCandidates.get(0);
			}
		} else {
			for (Value value : recordValues) {
				if (value.hasType(mapType) && !(value.hasType(nodeType) || value.hasType(
						relationshipType))) {
					queryRoot = value;
					break;
				}
			}
		}
		return queryRoot;
	}

	private Collection<String> createDynamicLabelsProperty(TypeInformation<?> type, Collection<String> dynamicLabels) {

		Collection<String> target = CollectionFactory.createCollection(type.getType(), String.class, dynamicLabels.size());
		target.addAll(dynamicLabels);
		return target;
	}

	@Override
	public void write(Object source, Map<String, Object> parameters) {
		Map<String, Object> properties = new HashMap<>();

		Neo4jPersistentEntity<?> nodeDescription = (Neo4jPersistentEntity<?>) nodeDescriptionStore
				.getNodeDescription(source.getClass());

		PersistentPropertyAccessor propertyAccessor = nodeDescription.getPropertyAccessor(source);
		nodeDescription.doWithProperties((Neo4jPersistentProperty p) -> {

			// Skip the internal properties, we don't want them to end up stored as properties
			if (p.isInternalIdProperty() || p.isDynamicLabels() || p.isEntity()) {
				return;
			}

			final Value value = conversionService.writeValue(propertyAccessor.getProperty(p), p.getTypeInformation(), p.getOptionalWritingConverter());
			if (p.isComposite()) {
				value.keys().forEach(k -> properties.put(k, value.get(k)));
			} else {
				properties.put(p.getPropertyName(), value);
			}
		});

		parameters.put(Constants.NAME_OF_PROPERTIES_PARAM, properties);

		// in case of relationship properties ignore internal id property
		if (nodeDescription.hasIdProperty()) {
			Neo4jPersistentProperty idProperty = nodeDescription.getRequiredIdProperty();
			parameters.put(Constants.NAME_OF_ID,
					conversionService.writeValue(propertyAccessor.getProperty(idProperty), idProperty.getTypeInformation(), idProperty.getOptionalWritingConverter()));
		}
		// in case of relationship properties ignore internal id property
		if (nodeDescription.hasVersionProperty()) {
			Long versionProperty = (Long) propertyAccessor.getProperty(nodeDescription.getRequiredVersionProperty());

			// we incremented this upfront the persist operation so the matching version would be one "before"
			parameters.put(Constants.NAME_OF_VERSION_PARAM, versionProperty - 1);
		}
	}

	/**
	 * Merges the root node of a query and the remaining record into one map, adding the internal ID of the node, too.
	 * Merge happens only when the record contains additional values.
	 *
	 * @param node Node whose attributes are about to be merged
	 * @param record Record that should be merged
	 * @return
	 */
	private static MapAccessor mergeRootNodeWithRecord(Node node, MapAccessor record) {
		Map<String, Object> mergedAttributes = new HashMap<>(node.size() + record.size() + 1);

		mergedAttributes.put(Constants.NAME_OF_INTERNAL_ID, node.id());
		mergedAttributes.putAll(node.asMap(Function.identity()));
		mergedAttributes.putAll(record.asMap(Function.identity()));

		return Values.value(mergedAttributes);
	}

	/**
	 * @param queryResult The original query result or a reduced form like a node or similar
	 * @param allValues The original query result
	 * @param nodeDescription The node description of the current entity to be mapped from the result
	 * @param <ET> As in entity type
	 * @return The mapped entity
	 */
	private <ET> ET map(MapAccessor queryResult, MapAccessor allValues, Neo4jPersistentEntity<ET> nodeDescription) {
		return map(queryResult, allValues, nodeDescription, null);
	}

	private <ET> ET map(MapAccessor queryResult, MapAccessor allValues, Neo4jPersistentEntity<ET> nodeDescription,
			@Nullable Object lastMappedEntity) {

		// if the given result does not contain an identifier to the mapped object cannot get temporarily saved
		Long internalId = getInternalId(queryResult);

		Supplier<Object> mappedObjectSupplier = () -> {

			List<String> allLabels = getLabels(queryResult, nodeDescription);
			NodeDescriptionAndLabels nodeDescriptionAndLabels = NodeDescriptionStore
					.deriveConcreteNodeDescription(nodeDescription, allLabels);
			Neo4jPersistentEntity<ET> concreteNodeDescription = (Neo4jPersistentEntity<ET>) nodeDescriptionAndLabels
					.getNodeDescription();

			Predicate<String> includeAllFields = (field) -> true;

			Collection<RelationshipDescription> relationships = CypherGenerator
					.getRelationshipDescriptionsUpAndDown(nodeDescription, includeAllFields);

			ET instance = instantiate(concreteNodeDescription, queryResult, allValues, relationships,
					nodeDescriptionAndLabels.getDynamicLabels(), lastMappedEntity);

			PersistentPropertyAccessor<ET> propertyAccessor = concreteNodeDescription.getPropertyAccessor(instance);

			if (concreteNodeDescription.requiresPropertyPopulation()) {

				// Fill simple properties
				Predicate<Neo4jPersistentProperty> isConstructorParameter = concreteNodeDescription
						.getPersistenceConstructor()::isConstructorParameter;
				PropertyHandler<Neo4jPersistentProperty> handler = populateFrom(queryResult, propertyAccessor,
						isConstructorParameter, nodeDescriptionAndLabels.getDynamicLabels(), lastMappedEntity);
				concreteNodeDescription.doWithProperties(handler);

				// in a cyclic graph / with bidirectional relationships, we could end up in a state in which we
				// reference the start again. Because it is getting still constructed, it won't be in the knownObjects
				// store unless we temporarily put it there.
				knownObjects.storeObject(internalId, instance);
				// Fill associations
				concreteNodeDescription.doWithAssociations(
						populateFrom(queryResult, allValues, propertyAccessor, isConstructorParameter, relationships));
			}
			ET bean = propertyAccessor.getBean();

			// save final state of the bean
			knownObjects.storeObject(internalId, bean);
			return bean;
		};

		Object mappedObject = knownObjects.getObject(internalId);
		if (mappedObject == null) {
			mappedObject = mappedObjectSupplier.get();
			knownObjects.storeObject(internalId, mappedObject);
		}
		return (ET) mappedObject;
	}

	@Nullable
	private Long getInternalId(@NonNull MapAccessor queryResult) {
		return queryResult instanceof Node
				? (Long) ((Node) queryResult).id()
				: queryResult.get(Constants.NAME_OF_INTERNAL_ID) == null || queryResult.get(Constants.NAME_OF_INTERNAL_ID).isNull()
				? null
				: queryResult.get(Constants.NAME_OF_INTERNAL_ID).asLong();
	}

	/**
	 * Returns the list of labels for the entity to be created from the "main" node returned.
	 *
	 * @param queryResult The complete query result
	 * @return The list of labels defined by the query variable {@link Constants#NAME_OF_LABELS}.
	 */
	@NonNull
	private List<String> getLabels(MapAccessor queryResult, @Nullable NodeDescription<?> nodeDescription) {
		Value labelsValue = queryResult.get(Constants.NAME_OF_LABELS);
		List<String> labels = new ArrayList<>();
		if (!labelsValue.isNull()) {
			labels = labelsValue.asList(Value::asString);
		} else if (queryResult instanceof Node) {
			Node nodeRepresentation = (Node) queryResult;
			nodeRepresentation.labels().forEach(labels::add);
		} else if (nodeDescription != null) {
			labels.addAll(nodeDescription.getStaticLabels());
		}
		return labels;
	}

	private <ET> ET instantiate(Neo4jPersistentEntity<ET> nodeDescription, MapAccessor values, MapAccessor allValues,
			Collection<RelationshipDescription> relationships, Collection<String> surplusLabels,
			Object lastMappedEntity) {

		ParameterValueProvider<Neo4jPersistentProperty> parameterValueProvider = new ParameterValueProvider<Neo4jPersistentProperty>() {
			@Override
			public Object getParameterValue(PreferredConstructor.Parameter parameter) {

				Neo4jPersistentProperty matchingProperty = nodeDescription.getRequiredPersistentProperty(parameter.getName());

				if (matchingProperty.isRelationship()) {
					return createInstanceOfRelationships(matchingProperty, values, allValues, relationships).orElse(null);
				} else if (matchingProperty.isDynamicLabels()) {
					return createDynamicLabelsProperty(matchingProperty.getTypeInformation(), surplusLabels);
				} else if (matchingProperty.isEntityInRelationshipWithProperties()) {
					return lastMappedEntity;
				}
				return conversionService.readValue(extractValueOf(matchingProperty, values), parameter.getType(), matchingProperty.getOptionalReadingConverter());
			}
		};

		return entityInstantiators.getInstantiatorFor(nodeDescription).createInstance(nodeDescription, parameterValueProvider);
	}

	private PropertyHandler<Neo4jPersistentProperty> populateFrom(MapAccessor queryResult,
			PersistentPropertyAccessor<?> propertyAccessor, Predicate<Neo4jPersistentProperty> isConstructorParameter,
			Collection<String> surplusLabels, Object targetNode) {
		return property -> {
			if (isConstructorParameter.test(property)) {
				return;
			}

			if (property.isDynamicLabels()) {
				propertyAccessor.setProperty(property,
						createDynamicLabelsProperty(property.getTypeInformation(), surplusLabels));
			} else if (property.isAnnotationPresent(TargetNode.class)) {
				if (queryResult instanceof Relationship) {
					propertyAccessor.setProperty(property, targetNode);
				}
			} else {
				propertyAccessor.setProperty(property,
						conversionService.readValue(extractValueOf(property, queryResult), property.getTypeInformation(), property.getOptionalReadingConverter()));
			}
		};
	}

	private AssociationHandler<Neo4jPersistentProperty> populateFrom(MapAccessor queryResult, MapAccessor allValues,
			PersistentPropertyAccessor<?> propertyAccessor, Predicate<Neo4jPersistentProperty> isConstructorParameter,
			Collection<RelationshipDescription> relationshipDescriptions) {
		return association -> {

			Neo4jPersistentProperty persistentProperty = association.getInverse();
			if (isConstructorParameter.test(persistentProperty)) {
				return;
			}

			createInstanceOfRelationships(persistentProperty, queryResult, allValues, relationshipDescriptions)
					.ifPresent(value -> propertyAccessor.setProperty(persistentProperty, value));
		};
	}

	private Optional<Object> createInstanceOfRelationships(Neo4jPersistentProperty persistentProperty, MapAccessor values,
			MapAccessor allValues, Collection<RelationshipDescription> relationshipDescriptions) {

		RelationshipDescription relationshipDescription = relationshipDescriptions.stream()
				.filter(r -> r.getFieldName().equals(persistentProperty.getName())).findFirst().get();

		String typeOfRelationship = relationshipDescription.getType();
		String sourceLabel = relationshipDescription.getSource().getPrimaryLabel();
		String targetLabel = relationshipDescription.getTarget().getPrimaryLabel();

		Neo4jPersistentEntity<?> genericTargetNodeDescription = (Neo4jPersistentEntity<?>) relationshipDescription
				.getTarget();

		List<String> allLabels = getLabels(values, null);
		NodeDescriptionAndLabels nodeDescriptionAndLabels = NodeDescriptionStore
				.deriveConcreteNodeDescription(genericTargetNodeDescription, allLabels);
		Neo4jPersistentEntity<?> concreteTargetNodeDescription = (Neo4jPersistentEntity<?>) nodeDescriptionAndLabels
				.getNodeDescription();

		List<Object> value = new ArrayList<>();
		Map<Object, Object> dynamicValue = new HashMap<>();

		BiConsumer<String, Object> mappedObjectHandler;
		Function<String, ?> keyTransformer;
		if (persistentProperty.isDynamicAssociation() && persistentProperty.getComponentType().isEnum()) {
			keyTransformer = f -> conversionService.convert(f, persistentProperty.getComponentType());
		} else {
			keyTransformer = Function.identity();
		}
		if (persistentProperty.isDynamicOneToManyAssociation()) {

			TypeInformation<?> actualType = persistentProperty.getTypeInformation().getRequiredActualType();
			mappedObjectHandler = (type, mappedObject) -> {
				List<Object> bucket = (List<Object>) dynamicValue.computeIfAbsent(keyTransformer.apply(type),
						s -> CollectionFactory.createCollection(actualType.getType(), persistentProperty.getAssociationTargetType(), values.size()));
				bucket.add(mappedObject);
			};
		} else if (persistentProperty.isDynamicAssociation()) {
			mappedObjectHandler = (type, mappedObject) -> dynamicValue.put(keyTransformer.apply(type), mappedObject);
		} else {
			mappedObjectHandler = (type, mappedObject) -> value.add(mappedObject);
		}

		String collectionName =
				relationshipDescription.generateRelatedNodesCollectionName(relationshipDescription.getSource());

		Value list = values.get(collectionName);

		List<Object> relationshipsAndProperties = new ArrayList<>();

		if (Values.NULL.equals(list)) {

			Collection<Relationship> allMatchingTypeRelationshipsInResult = new ArrayList<>();
			Collection<Node> allNodesWithMatchingLabelInResult = new ArrayList<>();

			// Grab everything else
			StreamSupport.stream(allValues.values().spliterator(), false)
					.filter(MappingSupport.isListContainingOnly(listType, this.relationshipType))
					.flatMap(entry -> entry.asList(Value::asRelationship).stream())
					.filter(r -> r.type().equals(typeOfRelationship) || relationshipDescription.isDynamic())
					.forEach(allMatchingTypeRelationshipsInResult::add);

			StreamSupport.stream(allValues.values().spliterator(), false)
					.filter(MappingSupport.isListContainingOnly(listType, this.nodeType))
					.flatMap(entry -> entry.asList(Value::asNode).stream())
					.filter(n -> n.hasLabel(targetLabel)).collect(Collectors.toList())
					.forEach(allNodesWithMatchingLabelInResult::add);

			// TODO OR
			if (allNodesWithMatchingLabelInResult.isEmpty() && allMatchingTypeRelationshipsInResult.isEmpty()) {
				return Optional.empty();
			}

			Function<Relationship, Long> targetIdSelector = relationshipDescription.isIncoming() ? Relationship::startNodeId : Relationship::endNodeId;
			Function<Relationship, Long> sourceIdSelector = relationshipDescription.isIncoming() ? Relationship::endNodeId : Relationship::startNodeId;
			Long sourceNodeId = getInternalId(values);
			for (Node possibleValueNode : allNodesWithMatchingLabelInResult) {
				long targetNodeId = possibleValueNode.id();

				for (Relationship possibleRelationship : allMatchingTypeRelationshipsInResult) {
					if (targetIdSelector.apply(possibleRelationship) == targetNodeId && sourceIdSelector.apply(possibleRelationship).equals(sourceNodeId)) {
						Object mappedObject = map(possibleValueNode, allValues, concreteTargetNodeDescription);
						if (relationshipDescription.hasRelationshipProperties()) {

							Object relationshipProperties = map(possibleRelationship, allValues,
									(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
									mappedObject);
							relationshipsAndProperties.add(relationshipProperties);
							mappedObjectHandler.accept(possibleRelationship.type(), relationshipProperties);
						} else {
							mappedObjectHandler.accept(possibleRelationship.type(), mappedObject);
						}
						allMatchingTypeRelationshipsInResult.remove(possibleRelationship);
						break;
					}
				}
			}
		} else {
			for (Value relatedEntity : list.asList(Function.identity())) {

				Object valueEntry = map(relatedEntity, allValues, concreteTargetNodeDescription);

				if (relationshipDescription.hasRelationshipProperties()) {
					String relationshipSymbolicName = sourceLabel
													  + RelationshipDescription.NAME_OF_RELATIONSHIP + targetLabel;
					Relationship relatedEntityRelationship = relatedEntity.get(relationshipSymbolicName)
							.asRelationship();

					Object relationshipProperties = map(relatedEntityRelationship, allValues,
							(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
							valueEntry);
					relationshipsAndProperties.add(relationshipProperties);
					mappedObjectHandler.accept(relatedEntity.get(RelationshipDescription.NAME_OF_RELATIONSHIP_TYPE).asString(), relationshipProperties);
				} else {
					mappedObjectHandler.accept(relatedEntity.get(RelationshipDescription.NAME_OF_RELATIONSHIP_TYPE).asString(),
							valueEntry);
				}
			}
		}

		if (persistentProperty.getTypeInformation().isCollectionLike()) {
			List<Object> returnedValues = relationshipDescription.hasRelationshipProperties() ?  relationshipsAndProperties : value;
			Collection<Object> target = CollectionFactory.createCollection(persistentProperty.getRawType(), persistentProperty.getComponentType(), returnedValues.size());
			target.addAll(returnedValues);
			return Optional.of(target);
		} else {
			if (relationshipDescription.isDynamic()) {
				return Optional.ofNullable(dynamicValue.isEmpty() ? null : dynamicValue);
			} else if (relationshipDescription.hasRelationshipProperties()) {
				return Optional.ofNullable(relationshipsAndProperties.isEmpty() ? null : relationshipsAndProperties.get(0));
			} else {
				return Optional.ofNullable(value.isEmpty() ? null : value.get(0));
			}
		}
	}

	private static Value extractValueOf(Neo4jPersistentProperty property, MapAccessor propertyContainer) {
		if (property.isInternalIdProperty()) {
			return propertyContainer instanceof Node ? Values.value(((Node) propertyContainer).id())
					: propertyContainer.get(Constants.NAME_OF_INTERNAL_ID);
		} else if (property.isComposite()) {
			String prefix = property.computePrefixWithDelimiter();

			if (propertyContainer.containsKey(Constants.NAME_OF_ALL_PROPERTIES)) {
				return extractCompositePropertyValues(propertyContainer.get(Constants.NAME_OF_ALL_PROPERTIES), prefix);
			} else {
				return extractCompositePropertyValues(propertyContainer, prefix);
			}
		} else {
			String graphPropertyName = property.getPropertyName();
			return propertyContainer.get(graphPropertyName);
		}
	}

	private static Value extractCompositePropertyValues(MapAccessor propertyContainer, String prefix) {
		Map<String, Value>  hlp = new HashMap<>(propertyContainer.size());
		propertyContainer.keys().forEach(k -> {
			if (k.startsWith(prefix)) {
				hlp.put(k, propertyContainer.get(k));
			}
		});
		return Values.value(hlp);
	}

	static class KnownObjects {

		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		private final Lock read = lock.readLock();
		private final Lock write = lock.writeLock();

		private final Map<Long, Object> internalIdStore = new HashMap<>();

		private void storeObject(@Nullable Long internalId, Object object) {
			if (internalId == null) {
				return;
			}
			try {
				write.lock();
				internalIdStore.put(internalId, object);
			} finally {
				write.unlock();
			}
		}

		@Nullable
		private Object getObject(@Nullable Long internalId) {
			if (internalId == null) {
				return null;
			}
			try {

				read.lock();

				Object knownEntity = internalIdStore.get(internalId);

				if (knownEntity != null) {
					return knownEntity;
				}

			} finally {
				read.unlock();
			}
			return null;
		}
	}
}
