/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import org.springframework.data.convert.EntityWriter;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.mapping.AssociationHandlerSupport;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.MappingSupport.RelationshipPropertiesWithEntityHolder;
import org.springframework.data.neo4j.core.mapping.Neo4jEntityConverter;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NestedRelationshipContext;
import org.springframework.data.neo4j.core.mapping.PropertyHandlerSupport;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * A specialized version of an {@link EntityWriter} for Neo4j that traverses the entity
 * and maps the entity, its association and other meta attributes into a couple of nested
 * maps. The values in the map will either be other maps or Neo4j Driver
 * {@link org.neo4j.driver.Value values}.
 *
 * @author Michael J. Simons
 * @since 6.1.0
 */
@API(status = API.Status.INTERNAL, since = "6.1.0")
final class Neo4jNestedMapEntityWriter implements EntityWriter<Object, Map<String, Object>> {

	private final Neo4jMappingContext mappingContext;

	private final Neo4jConversionService conversionService;

	private Neo4jNestedMapEntityWriter(Neo4jMappingContext mappingContext) {

		this.mappingContext = mappingContext;
		this.conversionService = mappingContext.getConversionService();
	}

	static EntityWriter<Object, Map<String, Object>> forContext(Neo4jMappingContext context) {
		return new Neo4jNestedMapEntityWriter(context);
	}

	@Override
	public void write(Object source, Map<String, Object> sink) {

		if (source == null) {
			return;
		}

		Set<Object> seenObjects = new HashSet<>();
		writeImpl(source, sink, seenObjects, true);
	}

	Map<String, Object> writeImpl(@Nullable Object source, Map<String, Object> sink, Set<Object> seenObjects,
			boolean initialObject) {

		if (source == null) {
			return sink;
		}
		Class<?> sourceType = source.getClass();
		if (!this.mappingContext.hasPersistentEntityFor(sourceType)) {
			throw new MappingException("Cannot write unknown entity of type '" + sourceType.getName() + "' into a map");
		}

		Neo4jPersistentEntity<?> entity = this.mappingContext.getRequiredPersistentEntity(sourceType);
		PersistentPropertyAccessor<Object> propertyAccessor = entity.getPropertyAccessor(source);
		Neo4jPersistentProperty idProperty = entity.getIdProperty();

		if (seenObjects.contains(source)) {
			// The ID property is null in case of relationship properties
			if (idProperty != null) {
				Value idValue = this.mappingContext.getConversionService()
					.writeValue(propertyAccessor.getProperty(idProperty), idProperty.getTypeInformation(), null);
				sink.put("__ref__", idValue);
			}
			return sink;
		}

		seenObjects.add(source);

		Neo4jEntityConverter delegate = this.mappingContext.getEntityConverter();
		delegate.write(source, sink);

		addLabels(sink, entity, propertyAccessor);
		addRelations(sink, entity, propertyAccessor, seenObjects);
		if (initialObject && entity.isRelationshipPropertiesEntity()) {
			PropertyHandlerSupport.of(entity).doWithProperties(p -> {
				if (p.isAnnotationPresent(TargetNode.class)) {
					Value target = Values
						.value(this.writeImpl(propertyAccessor.getProperty(p), new HashMap<>(), seenObjects, false));
					sink.put("__target__", target);
				}
			});
		}

		// Remove redundant values
		// Internal ID
		if (!(idProperty == null || idProperty.isInternalIdProperty())) {
			@SuppressWarnings("unchecked")
			Map<String, Object> propertyMap = (Map<String, Object>) sink.get(Constants.NAME_OF_PROPERTIES_PARAM);
			if (propertyMap != null) {
				propertyMap.remove(idProperty.getPropertyName());
			}
		}

		// Param not needed
		sink.remove(Constants.NAME_OF_VERSION_PARAM);

		return sink;
	}

	private void addRelations(Map<String, Object> sink, Neo4jPersistentEntity<?> entity,
			PersistentPropertyAccessor<Object> propertyAccessor, Set<Object> seenObjects) {

		@SuppressWarnings("unchecked")
		Map<String, Object> propertyMap = (Map<String, Object>) sink.get(Constants.NAME_OF_PROPERTIES_PARAM);
		AssociationHandlerSupport.of(entity).doWithAssociations(association -> {

			NestedRelationshipContext context = NestedRelationshipContext.of(association, propertyAccessor, entity);
			RelationshipDescription description = (RelationshipDescription) association;
			Neo4jPersistentProperty property = association.getInverse();

			// Not using the Mapping support here so that we don't have to deal with the
			// nested array lists.
			Collection<?> unifiedView = Optional.ofNullable(context.getValue())
				.map(v -> (v instanceof Collection<?> col) ? col : Collections.singletonList(v))
				.orElseGet(Collections::emptyList);

			if (property.isDynamicAssociation()) {
				TypeInformation<?> keyType = property.getTypeInformation().getRequiredComponentType();

				Map<String, Value> collect = unifiedView.stream()
					.filter(Objects::nonNull)
					.flatMap(intoSingleMapEntries())
					.flatMap(intoSingleCollectionEntries())
					.map(relatedEntry -> {
						String key = this.conversionService
							.writeValue(relatedEntry.getKey(), keyType, property.getOptionalConverter())
							.asString();

						Map<String, Object> relatedObjectProperties;
						Object relatedObject = relatedEntry.getValue();
						relatedObjectProperties = extractPotentialRelationProperties(description, relatedObject,
								seenObjects);

						return new HashMap.SimpleEntry<>(key, relatedObjectProperties);
					})
					.collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue,
							Collectors.collectingAndThen(Collectors.toList(), Values::value))));
				if (!collect.isEmpty() && propertyMap != null) {
					propertyMap.putAll(collect);
				}
			}
			else {
				List<Object> relatedObjects = unifiedView.stream()
					.filter(Objects::nonNull)
					.map(relatedObject -> extractPotentialRelationProperties(description, relatedObject, seenObjects))
					.collect(Collectors.toList());

				if (!relatedObjects.isEmpty() && propertyMap != null) {
					String type = description.getType();
					if (propertyMap.containsKey(type)) {
						Value v = (Value) propertyMap.get(type);
						relatedObjects.addAll(v.asList(Function.identity()));
					}
					propertyMap.put(type, Values.value(relatedObjects));
				}
			}
		});
	}

	private Function<Map.Entry<?, ?>, Stream<? extends Map.Entry<?, ?>>> intoSingleCollectionEntries() {
		return e -> {
			if (e.getValue() instanceof Collection<?> col) {
				return col.stream().map(v -> new AbstractMap.SimpleEntry<>(e.getKey(), v));
			}
			else {
				return Stream.of(e);
			}
		};
	}

	private Function<Object, Stream<? extends Map.Entry<?, ?>>> intoSingleMapEntries() {
		return e -> {
			if (e instanceof Map<?, ?> map) {
				return map.entrySet().stream();
			}
			else {
				return Stream.of((Map.Entry<?, ?>) e);
			}
		};
	}

	private void addLabels(Map<String, Object> sink, Neo4jPersistentEntity<?> entity,
			PersistentPropertyAccessor<Object> propertyAccessor) {

		if (entity.isRelationshipPropertiesEntity()) {
			return;
		}

		List<String> labels = new ArrayList<>();
		labels.add(entity.getPrimaryLabel());
		entity.getDynamicLabelsProperty().map(p -> {
			@SuppressWarnings("unchecked")
			Collection<String> propertyValue = (Collection<String>) propertyAccessor.getProperty(p);
			return propertyValue;
		}).ifPresent(labels::addAll);
		sink.put(Constants.NAME_OF_ALL_LABELS, Values.value(labels));
	}

	private Map<String, Object> extractPotentialRelationProperties(RelationshipDescription description,
			Object relatedObject, Set<Object> seenObjects) {

		if (!description.hasRelationshipProperties()) {
			return this.writeImpl(relatedObject, new HashMap<>(), seenObjects, false);
		}

		RelationshipPropertiesWithEntityHolder tuple = (RelationshipPropertiesWithEntityHolder) relatedObject;
		Map<String, Object> relatedObjectProperties;
		relatedObjectProperties = this.writeImpl(tuple.getRelationshipProperties(), new HashMap<>(), seenObjects,
				false);
		relatedObjectProperties.put("__target__",
				this.writeImpl(tuple.getRelatedEntity(), new HashMap<>(), seenObjects, false));
		return relatedObjectProperties;
	}

}
