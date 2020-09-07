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
import org.neo4j.driver.types.Relationship;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.log.LogAccessor;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.EntityInstantiators;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.convert.Neo4jConverter;
import org.springframework.data.neo4j.core.schema.Constants;
import org.springframework.data.neo4j.core.schema.RelationshipDescription;
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
final class DefaultNeo4jConverter implements Neo4jConverter {

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(DefaultNeo4jConverter.class));

	/**
	 * The shared entity instantiators of this context. Those should not be recreated for each entity or even not for each
	 * query, as otherwise the cache of Spring's org.springframework.data.convert.ClassGeneratingEntityInstantiator won't
	 * apply
	 */
	private static final EntityInstantiators INSTANTIATORS = new EntityInstantiators();

	private final NodeDescriptionStore nodeDescriptionStore;
	private final ConversionService conversionService;

	private TypeSystem typeSystem;

	DefaultNeo4jConverter(Neo4jConversions neo4jConversions, NodeDescriptionStore nodeDescriptionStore) {

		Assert.notNull(neo4jConversions, "Neo4jConversions must not be null!");

		final ConfigurableConversionService configurableConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(configurableConversionService);

		this.conversionService = configurableConversionService;
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
				return map(queryRoot, rootNodeDescription, new KnownObjects());
			}
		} catch (Exception e) {
			throw new MappingException("Error mapping " + record.toString(), e);
		}
	}

	@Override
	@Nullable
	public Object readValueForProperty(@Nullable Value value, TypeInformation<?> type) {

		boolean valueIsLiteralNullOrNullValue = value == null || value == Values.NULL;

		try {
			Class<?> rawType = type.getType();

			if (!valueIsLiteralNullOrNullValue && isCollection(type)) {
				Collection<Object> target = CollectionFactory
						.createCollection(rawType, type.getComponentType().getType(), value.size());
				value.values()
						.forEach(element -> target.add(conversionService.convert(element, type.getComponentType().getType())));
				return target;
			}

			return conversionService.convert(valueIsLiteralNullOrNullValue ? null : value, rawType);
		} catch (Exception e) {
			String msg = String.format("Could not convert %s into %s", value, type.toString());
			throw new TypeMismatchDataAccessException(msg, e);
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

			final Object value = writeValueFromProperty(propertyAccessor.getProperty(p), p.getTypeInformation());
			properties.put(p.getPropertyName(), value);
		});

		parameters.put(Constants.NAME_OF_PROPERTIES_PARAM, properties);

		// in case of relationship properties ignore internal id property
		if (nodeDescription.hasIdProperty()) {
			Neo4jPersistentProperty idProperty = nodeDescription.getRequiredIdProperty();
			parameters.put(Constants.NAME_OF_ID,
					writeValueFromProperty(propertyAccessor.getProperty(idProperty), idProperty.getTypeInformation()));
		}
		// in case of relationship properties ignore internal id property
		if (nodeDescription.hasVersionProperty()) {
			Long versionProperty = (Long) propertyAccessor.getProperty(nodeDescription.getRequiredVersionProperty());

			// we incremented this upfront the persist operation so the matching version would be one "before"
			parameters.put(Constants.NAME_OF_VERSION_PARAM, versionProperty - 1);
		}
	}

	@Override
	public Value writeValueFromProperty(@Nullable Object value, TypeInformation<?> type) {

		if (value == null) {
			return Values.NULL;
		}

		if (isCollection(type)) {
			Collection<?> sourceCollection = (Collection<?>) value;
			Object[] targetCollection = (sourceCollection).stream()
					.map(element -> conversionService.convert(element, Value.class)).toArray();
			return Values.value(targetCollection);
		}

		return conversionService.convert(value, Value.class);
	}

	private static boolean isCollection(TypeInformation<?> type) {
		return Collection.class.isAssignableFrom(type.getType());
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
	private <ET> ET map(MapAccessor queryResult, Neo4jPersistentEntity<ET> nodeDescription, KnownObjects knownObjects) {

		List<String> allLabels = getLabels(queryResult);
		NodeDescriptionAndLabels nodeDescriptionAndLabels = nodeDescriptionStore
				.deriveConcreteNodeDescription(nodeDescription, allLabels);
		Neo4jPersistentEntity<ET> concreteNodeDescription = (Neo4jPersistentEntity<ET>) nodeDescriptionAndLabels
				.getNodeDescription();

		Collection<RelationshipDescription> relationships = concreteNodeDescription.getRelationships();

		ET instance = instantiate(concreteNodeDescription, queryResult, knownObjects, relationships,
				nodeDescriptionAndLabels.getDynamicLabels());

		PersistentPropertyAccessor<ET> propertyAccessor = concreteNodeDescription.getPropertyAccessor(instance);

		if (concreteNodeDescription.requiresPropertyPopulation()) {

			// Fill simple properties
			Predicate<Neo4jPersistentProperty> isConstructorParameter = concreteNodeDescription
					.getPersistenceConstructor()::isConstructorParameter;
			PropertyHandler<Neo4jPersistentProperty> handler = populateFrom(queryResult, knownObjects, propertyAccessor,
					isConstructorParameter, nodeDescriptionAndLabels.getDynamicLabels());
			concreteNodeDescription.doWithProperties(handler);

			// Fill associations
			concreteNodeDescription.doWithAssociations(
					populateFrom(queryResult, propertyAccessor, isConstructorParameter, relationships, knownObjects));
		}
		return propertyAccessor.getBean();
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
			Collection<RelationshipDescription> relationships, Collection<String> surplusLabels) {

		ParameterValueProvider<Neo4jPersistentProperty> parameterValueProvider = new ParameterValueProvider<Neo4jPersistentProperty>() {
			@Override
			public Object getParameterValue(PreferredConstructor.Parameter parameter) {

				Neo4jPersistentProperty matchingProperty = nodeDescription.getRequiredPersistentProperty(parameter.getName());

				if (matchingProperty.isRelationship()) {
					return createInstanceOfRelationships(matchingProperty, values, knownObjects, relationships).orElse(null);
				} else if (matchingProperty.isDynamicLabels()) {
					return createDynamicLabelsProperty(matchingProperty.getTypeInformation(), surplusLabels);
				}
				return readValueForProperty(extractValueOf(matchingProperty, values), parameter.getType());
			}
		};

		return INSTANTIATORS.getInstantiatorFor(nodeDescription).createInstance(nodeDescription, parameterValueProvider);
	}

	private PropertyHandler<Neo4jPersistentProperty> populateFrom(MapAccessor queryResult, KnownObjects knownObjects,
			PersistentPropertyAccessor<?> propertyAccessor, Predicate<Neo4jPersistentProperty> isConstructorParameter,
			Collection<String> surplusLabels) {
		return property -> {
			if (isConstructorParameter.test(property)) {
				return;
			}

			if (property.isDynamicLabels()) {
				propertyAccessor.setProperty(property,
						createDynamicLabelsProperty(property.getTypeInformation(), surplusLabels));
			} else if (property.isAnnotationPresent(TargetNode.class)) {
				if (queryResult instanceof Relationship) {
					Relationship resultRelationship = (Relationship) queryResult;
					// bad hack atm because we do not know which direction the relationship is defined here.
					Object object = knownObjects.getObject(resultRelationship.endNodeId());
					if (object == null || !property.getType().isAssignableFrom(object.getClass())) {
						object = knownObjects.getObject(resultRelationship.startNodeId());
					}

					propertyAccessor.setProperty(property, object);
				} else {
					throw new RuntimeException("Alerta");
				}
			} else {
				propertyAccessor.setProperty(property,
						readValueForProperty(extractValueOf(property, queryResult), property.getTypeInformation()));
			}
		};
	}

	private AssociationHandler<Neo4jPersistentProperty> populateFrom(MapAccessor queryResult,
			PersistentPropertyAccessor<?> propertyAccessor, Predicate<Neo4jPersistentProperty> isConstructorParameter,
			Collection<RelationshipDescription> relationships, KnownObjects knownObjects) {
		return association -> {

			Neo4jPersistentProperty persistentProperty = association.getInverse();
			if (isConstructorParameter.test(persistentProperty)) {
				return;
			}

			createInstanceOfRelationships(persistentProperty, queryResult, knownObjects, relationships)
					.ifPresent(value -> propertyAccessor.setProperty(persistentProperty, value));
		};
	}

	private Optional<Object> createInstanceOfRelationships(Neo4jPersistentProperty persistentProperty, MapAccessor values,
			KnownObjects knownObjects, Collection<RelationshipDescription> relationshipDescriptions) {

		RelationshipDescription relationshipDescription = relationshipDescriptions.stream()
				.filter(r -> r.getFieldName().equals(persistentProperty.getName())).findFirst().get();

		String relationshipType = relationshipDescription.getType();
		String targetLabel = relationshipDescription.getTarget().getPrimaryLabel();

		Neo4jPersistentEntity<?> genericTargetNodeDescription = (Neo4jPersistentEntity<?>) relationshipDescription
				.getTarget();

		List<String> allLabels = getLabels(values);
		NodeDescriptionAndLabels nodeDescriptionAndLabels = nodeDescriptionStore
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

		// if the list is null the mapping is based on a custom query
		if (list == Values.NULL) {

			Predicate<Value> isList = entry -> entry instanceof Value && typeSystem.LIST().isTypeOf(entry);

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

			Function<Relationship, Long> targetIdSelector = relationshipDescription.isOutgoing() ? Relationship::endNodeId : Relationship::startNodeId;

			for (Node possibleValueNode : allNodesWithMatchingLabelInResult) {
				long nodeId = possibleValueNode.id();

				for (Relationship possibleRelationship : allMatchingTypeRelationshipsInResult) {
					if (targetIdSelector.apply(possibleRelationship) == nodeId) {
						Object mappedObject = knownObjects.computeIfAbsent(nodeId, nodeId, () -> map(possibleValueNode, concreteTargetNodeDescription, knownObjects));
						if (relationshipDescription.hasRelationshipProperties()) {

							Object relationshipProperties = map(possibleRelationship,
									(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
									knownObjects);
							relationshipsAndProperties.add(relationshipProperties); // and somehow mappedObject
						} else {
							mappedObjectHandler.accept(possibleRelationship.type(), mappedObject);
						}
						break;
					}
				}
			}
		} else {
			for (Value relatedEntity : list.asList(Function.identity())) {
				Neo4jPersistentProperty idProperty = concreteTargetNodeDescription.getRequiredIdProperty();

				// internal (generated) id or external set
				String relatedEntityIdKey = idProperty.isInternalIdProperty() ? Constants.NAME_OF_INTERNAL_ID
						: concreteTargetNodeDescription.getIdDescription().getOptionalGraphPropertyName()
								.orElse(idProperty.getName());
				Object idValue = relatedEntity.get(relatedEntityIdKey);
				Long internalIdValue = relatedEntity.get(Constants.NAME_OF_INTERNAL_ID).asLong();

				Object valueEntry = knownObjects.computeIfAbsent(idValue, internalIdValue,
						() -> map(relatedEntity, concreteTargetNodeDescription, knownObjects));

				if (relationshipDescription.hasRelationshipProperties()) {
					Relationship relatedEntityRelationship = relatedEntity.get(RelationshipDescription.NAME_OF_RELATIONSHIP)
							.asRelationship();

					Object relationshipProperties = map(relatedEntityRelationship,
							(Neo4jPersistentEntity) relationshipDescription.getRelationshipPropertiesEntity(),
							knownObjects);
					relationshipsAndProperties.add(relationshipProperties); // and somehow valueEntry;
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
				return Optional.of(relationshipsAndProperties);
			} else {
				return Optional.ofNullable(value.isEmpty() ? null : value.get(0));
			}
		}

	}

	private static Value extractValueOf(Neo4jPersistentProperty property, MapAccessor propertyContainer) {
		if (property.isInternalIdProperty()) {
			return propertyContainer instanceof Node ? Values.value(((Node) propertyContainer).id())
					: propertyContainer.get(Constants.NAME_OF_INTERNAL_ID);
		} else {
			String graphPropertyName = property.getPropertyName();
			return propertyContainer.get(graphPropertyName);
		}
	}

	static class KnownObjects {

		private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		private final Lock read = lock.readLock();
		private final Lock write = lock.writeLock();

		private final Map<Object, Object> store = new HashMap<>();
		private final Map<Long, Object> internalIdStore = new HashMap<>();

		Object computeIfAbsent(Object key, Long internalId, Supplier<Object> entitySupplier) {
			Object knownEntity = getObject(key);

			// if it is not in the store, it has to get re-computed also for the internalIdStore
			if (knownEntity != null) {
				return knownEntity;
			}

			try {
				write.lock();
				Object computedEntity = entitySupplier.get();
				store.put(key, computedEntity);
				internalIdStore.put(internalId, computedEntity);
				return computedEntity;
			} finally {
				write.unlock();
			}
		}

		@Nullable
		private Object getObject(Object key) {
			try {

				read.lock();

				Object knownEntity = store.get(key);

				if (knownEntity != null) {
					return knownEntity;
				}

			} finally {
				read.unlock();
			}
			return null;
		}

		@Nullable
		private Object getObject(Long internalId) {
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
