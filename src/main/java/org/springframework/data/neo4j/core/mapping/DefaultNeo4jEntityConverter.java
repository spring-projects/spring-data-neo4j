/*
 * Copyright 2011-2020 the original author or authors.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.CollectionFactory;
import org.springframework.core.log.LogAccessor;
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

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(DefaultNeo4jEntityConverter.class));

	/**
	 * The shared entity instantiators of this context. Those should not be recreated for each entity or even not for each
	 * query, as otherwise the cache of Spring's org.springframework.data.convert.ClassGeneratingEntityInstantiator won't
	 * apply
	 */
	private static final EntityInstantiators INSTANTIATORS = new EntityInstantiators();

	private final NodeDescriptionStore nodeDescriptionStore;
	private final Neo4jConversionService conversionService;

	private TypeSystem typeSystem;

	DefaultNeo4jEntityConverter(Neo4jConversionService conversionService, NodeDescriptionStore nodeDescriptionStore) {

		Assert.notNull(conversionService, "Neo4jConversionService must not be null!");

		this.conversionService = conversionService;
		this.nodeDescriptionStore = nodeDescriptionStore;
	}

	@Override
	public <R> R read(Class<R> targetType, Record record) {

		Neo4jPersistentEntity<R> rootNodeDescription = (Neo4jPersistentEntity) nodeDescriptionStore
				.getNodeDescription(targetType);

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
				return null; // todo should not be null because of the @nonnullapi annotation in the EntityReader. Fail?
			} else {
				return map(queryRoot, rootNodeDescription, new KnownObjects(), new HashSet<>());
			}
		} catch (Exception e) {
			throw new MappingException("Error mapping " + record.toString(), e);
		}
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

	void setTypeSystem(TypeSystem typeSystem) {
		this.typeSystem = typeSystem;
	}

	/**
	 * Merges the root node of a query and the remaining record into one map, adding the internal ID of the node, too.
	 * Merge happens only when the record contains additional values.
	 *
	 * @param node Node whose attributes are about to be merged
	 * @param record Record that should be merged
	 * @return
	 */
	private static MapAccessor mergeRootNodeWithRecord(Node node, Record record) {
		Map<String, Object> mergedAttributes = new HashMap<>(node.size() + record.size() + 1);

		mergedAttributes.put(Constants.NAME_OF_INTERNAL_ID, node.id());
		mergedAttributes.putAll(node.asMap(Function.identity()));
		mergedAttributes.putAll(record.asMap(Function.identity()));

		return Values.value(mergedAttributes);
	}

	/**
	 * @param queryResult The original query result
	 * @param nodeDescription The node description of the current entity to be mapped from the result
	 * @param knownObjects The current list of known objects
	 * @param <ET> As in entity type
	 * @return
	 */
	private <ET> ET map(MapAccessor queryResult, Neo4jPersistentEntity<ET> nodeDescription, KnownObjects knownObjects, Set<Path.Segment> processedSegments) {
		return map(queryResult, nodeDescription, knownObjects, null, processedSegments);
	}

	/**
	 * @param queryResult The original query result
	 * @param nodeDescription The node description of the current entity to be mapped from the result
	 * @param knownObjects The current list of known objects
	 * @param lastMappedEntity Previous created entity for relationships, can be null.
	 * @param <ET> As in entity type
	 * @return
	 */
	private <ET> ET map(MapAccessor queryResult, Neo4jPersistentEntity<ET> nodeDescription, KnownObjects knownObjects,
						@Nullable Object lastMappedEntity, Set<Path.Segment> processedSegments) {

		// if the given result does not contain an identifier to the mapped object cannot get temporarily saved
		Long internalId = queryResult.get(Constants.NAME_OF_INTERNAL_ID).isNull()
			? null
			:	queryResult instanceof Node
				? ((Node) queryResult).id()
				: queryResult.get(Constants.NAME_OF_INTERNAL_ID).asLong();

		Supplier<Object> mappedObjectSupplier = () -> {

			List<String> allLabels = getLabels(queryResult);
			NodeDescriptionAndLabels nodeDescriptionAndLabels = NodeDescriptionStore
					.deriveConcreteNodeDescription(nodeDescription, allLabels);
			Neo4jPersistentEntity<ET> concreteNodeDescription = (Neo4jPersistentEntity<ET>) nodeDescriptionAndLabels
					.getNodeDescription();

			Collection<RelationshipDescription> relationships = concreteNodeDescription.getRelationships();

			ET instance = instantiate(concreteNodeDescription, queryResult, knownObjects, relationships,
					nodeDescriptionAndLabels.getDynamicLabels(), processedSegments);

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
						populateFrom(queryResult, propertyAccessor, isConstructorParameter, relationships, knownObjects, processedSegments));
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

	/**
	 * Returns the list of labels for the entity to be created from the "main" node returned.
	 *
	 * @param queryResult The complete query result
	 * @return The list of labels defined by the query variable {@link Constants#NAME_OF_LABELS}.
	 */
	@NonNull
	private List<String> getLabels(MapAccessor queryResult) {
		Value labelsValue = queryResult.get(Constants.NAME_OF_LABELS);
		List<String> labels = new ArrayList<>();
		if (!labelsValue.isNull()) {
			labels = labelsValue.asList(Value::asString);
		} else if (queryResult instanceof Node) {
			Node nodeRepresentation = (Node) queryResult;
			nodeRepresentation.labels().forEach(labels::add);
		}
		return labels;
	}

	private <ET> ET instantiate(Neo4jPersistentEntity<ET> nodeDescription, MapAccessor values, KnownObjects knownObjects,
			Collection<RelationshipDescription> relationships, Collection<String> surplusLabels, Set<Path.Segment> processedSegments) {

		ParameterValueProvider<Neo4jPersistentProperty> parameterValueProvider = new ParameterValueProvider<Neo4jPersistentProperty>() {
			@Override
			public Object getParameterValue(PreferredConstructor.Parameter parameter) {

				Neo4jPersistentProperty matchingProperty = nodeDescription.getRequiredPersistentProperty(parameter.getName());

				if (matchingProperty.isRelationship()) {
					return createInstanceOfRelationships(matchingProperty, values, knownObjects, relationships, processedSegments).orElse(null);
				} else if (matchingProperty.isDynamicLabels()) {
					return createDynamicLabelsProperty(matchingProperty.getTypeInformation(), surplusLabels);
				}
				return conversionService.readValue(extractValueOf(matchingProperty, values), parameter.getType(), matchingProperty.getOptionalReadingConverter());
			}
		};

		return INSTANTIATORS.getInstantiatorFor(nodeDescription).createInstance(nodeDescription, parameterValueProvider);
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

	private AssociationHandler<Neo4jPersistentProperty> populateFrom(MapAccessor queryResult,
			PersistentPropertyAccessor<?> propertyAccessor, Predicate<Neo4jPersistentProperty> isConstructorParameter,
			Collection<RelationshipDescription> relationshipDescriptions, KnownObjects knownObjects, Set<Path.Segment> processedSegments) {
		return association -> {

			Neo4jPersistentProperty persistentProperty = association.getInverse();
			if (isConstructorParameter.test(persistentProperty)) {
				return;
			}

			createInstanceOfRelationships(persistentProperty, queryResult, knownObjects, relationshipDescriptions, processedSegments)
					.ifPresent(value -> propertyAccessor.setProperty(persistentProperty, value));
		};
	}

	private Optional<Object> createInstanceOfRelationships(Neo4jPersistentProperty persistentProperty, MapAccessor values,
			KnownObjects knownObjects, Collection<RelationshipDescription> relationshipDescriptions, Set<Path.Segment> processedSegments) {

		RelationshipDescription relationshipDescription = relationshipDescriptions.stream()
				.filter(r -> r.getFieldName().equals(persistentProperty.getName())).findFirst().get();

		String relationshipType = relationshipDescription.getType();
		String targetLabel = relationshipDescription.getTarget().getPrimaryLabel();

		Neo4jPersistentEntity<?> genericTargetNodeDescription = (Neo4jPersistentEntity<?>) relationshipDescription
				.getTarget();

		List<String> allLabels = getLabels(values);
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

		Value list = values.get(relationshipDescription.generateRelatedNodesCollectionName());

		List<Object> relationshipsAndProperties = new ArrayList<>();

		boolean isGeneratedPathBased = values.containsKey(Constants.NAME_OF_PATHS);

		Predicate<Value> isList = entry -> typeSystem.LIST().isTypeOf(entry);

		if (isGeneratedPathBased) {

			Value internalStartNodeIdValue = values.get(Constants.NAME_OF_INTERNAL_ID);
			long startNodeId = internalStartNodeIdValue.asLong();

			Predicate<Value> containsOnlyPaths = entry -> entry.asList(Function.identity()).stream()
					.allMatch(listEntry -> typeSystem.PATH().isTypeOf(listEntry));

			List<Path> allPaths = StreamSupport.stream(values.values().spliterator(), false)
					.filter(isList.and(containsOnlyPaths)).flatMap(entry -> entry.asList(Value::asPath).stream())
					.collect(Collectors.toList());

			List<Path.Segment> segments = allPaths.stream()
					.flatMap(p -> StreamSupport.stream(p.spliterator(), false))
					.filter(s -> s.start().id() == startNodeId
							&& (relationshipDescription.isIncoming() ? s.relationship().endNodeId() : s.relationship().startNodeId()) == startNodeId
							&& (s.relationship().hasType(relationshipType) || relationshipDescription.isDynamic())
							&& s.end().hasLabel(targetLabel))
					.distinct()
					.collect(Collectors.toList());

			for (Path.Segment segment : segments) {
				if (processedSegments.contains(segment)) {
					continue;
				}
				processedSegments.add(segment);
				Object mappedObject = map(extractNextNodeAndAppendPath(segment.end(), allPaths),
						concreteTargetNodeDescription, knownObjects, processedSegments);
				if (relationshipDescription.hasRelationshipProperties()) {

					Object relationshipProperties = map(segment.relationship(),
							(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
							knownObjects, mappedObject, processedSegments);
					relationshipsAndProperties.add(relationshipProperties);
					mappedObjectHandler.accept(segment.relationship().type(), relationshipProperties);
				} else {
					mappedObjectHandler.accept(segment.relationship().type(), mappedObject);
				}
			}
		} else if (Values.NULL.equals(list)) {

			Predicate<Value> containsOnlyRelationships = entry -> entry.asList(Function.identity()).stream()
					.allMatch(listEntry -> typeSystem.RELATIONSHIP().isTypeOf(listEntry));

			Predicate<Value> containsOnlyNodes = entry -> entry.asList(Function.identity()).stream()
					.allMatch(listEntry -> typeSystem.NODE().isTypeOf(listEntry));

			// find relationships in the result
			List<Relationship> allMatchingTypeRelationshipsInResult = StreamSupport
					.stream(values.values().spliterator(), false).filter(isList.and(containsOnlyRelationships))
					.flatMap(entry -> entry.asList(Value::asRelationship).stream()).filter(r -> r.type().equals(relationshipType))
					.collect(Collectors.toList());

			List<Node> allNodesWithMatchingLabelInResult = StreamSupport.stream(values.values().spliterator(), false)
					.filter(isList.and(containsOnlyNodes)).flatMap(entry -> entry.asList(Value::asNode).stream())
					.filter(n -> n.hasLabel(targetLabel)).collect(Collectors.toList());

			if (allNodesWithMatchingLabelInResult.isEmpty() && allMatchingTypeRelationshipsInResult.isEmpty()) {
				return Optional.empty();
			}

			Function<Relationship, Long> targetIdSelector = relationshipDescription.isIncoming() ? Relationship::startNodeId : Relationship::endNodeId;

			for (Node possibleValueNode : allNodesWithMatchingLabelInResult) {
				long nodeId = possibleValueNode.id();

				for (Relationship possibleRelationship : allMatchingTypeRelationshipsInResult) {
					if (targetIdSelector.apply(possibleRelationship) == nodeId) {
						Object mappedObject = map(possibleValueNode, concreteTargetNodeDescription, knownObjects, processedSegments);
						if (relationshipDescription.hasRelationshipProperties()) {

							Object relationshipProperties = map(possibleRelationship,
									(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
									knownObjects, mappedObject, processedSegments);
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

				Object valueEntry = map(relatedEntity, concreteTargetNodeDescription, knownObjects, processedSegments);

				if (relationshipDescription.hasRelationshipProperties()) {
					Relationship relatedEntityRelationship = relatedEntity.get(RelationshipDescription.NAME_OF_RELATIONSHIP)
							.asRelationship();

					Object relationshipProperties = map(relatedEntityRelationship,
							(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
							knownObjects, valueEntry, processedSegments);
					relationshipsAndProperties.add(relationshipProperties);
					mappedObjectHandler.accept(relatedEntity.get(RelationshipDescription.NAME_OF_RELATIONSHIP_TYPE).asString(), relationshipProperties);
				} else {
					mappedObjectHandler.accept(relatedEntity.get(RelationshipDescription.NAME_OF_RELATIONSHIP_TYPE).asString(),
							valueEntry);
				}
			}
		}

		if (persistentProperty.getTypeInformation().isCollectionLike()) {
			if (relationshipDescription.hasRelationshipProperties()) {
				return Optional.of(relationshipsAndProperties);
			} else if (persistentProperty.getType().equals(Set.class)) {
				return Optional.of(new HashSet(value));
			} else {
				return Optional.of(value);
			}
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

	private MapAccessor extractNextNodeAndAppendPath(Node possibleValueNode, List<Path> allPaths) {
		Map<String, Object> newQueryResult = new HashMap<>(possibleValueNode.asMap());
		newQueryResult.put(Constants.NAME_OF_INTERNAL_ID, possibleValueNode.id());
		newQueryResult.put(Constants.NAME_OF_LABELS, possibleValueNode.labels());

		newQueryResult.put(Constants.NAME_OF_PATHS, allPaths);
		return Values.value(newQueryResult);
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
