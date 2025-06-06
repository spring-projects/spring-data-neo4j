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
package org.springframework.data.neo4j.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.FunctionInvocation;
import org.neo4j.cypherdsl.core.Named;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.IdDescription;
import org.springframework.data.neo4j.core.mapping.IdentitySupport;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.core.mapping.PropertyTraverser;
import org.springframework.data.neo4j.core.mapping.SpringDataCypherDsl;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;

/**
 * Utilities for templates.
 *
 * @author Michael J. Simons
 * @since 6.0.9
 */
@API(status = API.Status.INTERNAL, since = "6.0.9")
public final class TemplateSupport {

	private TemplateSupport() {
	}

	@Nullable public static Class<?> findCommonElementType(Iterable<?> collection) {

		if (collection == null) {
			return null;
		}

		Collection<Class<?>> allClasses = StreamSupport.stream(collection.spliterator(), true)
			.filter(Objects::nonNull)
			.map(Object::getClass)
			.collect(Collectors.toSet());

		if (allClasses.isEmpty()) {
			return EmptyIterable.class;
		}

		Class<?> candidate = null;
		for (Class<?> type : allClasses) {
			if (candidate == null) {
				candidate = type;
			}
			else if (candidate != type) {
				candidate = null;
				break;
			}
		}

		if (candidate != null) {
			return candidate;
		}
		else {
			Predicate<Class<?>> moveUp = c -> c != null && c != Object.class;
			Set<Class<?>> mostAbstractClasses = new HashSet<>();
			for (Class<?> type : allClasses) {
				while (moveUp.test(type.getSuperclass())) {
					type = type.getSuperclass();
				}
				mostAbstractClasses.add(type);
			}
			candidate = (mostAbstractClasses.size() != 1) ? null : mostAbstractClasses.iterator().next();
		}

		if (candidate != null) {
			return candidate;
		}
		else {
			List<Set<Class<?>>> interfacesPerClass = allClasses.stream()
				.map(c -> Arrays.stream(c.getInterfaces()).collect(Collectors.toSet()))
				.collect(Collectors.toList());
			Set<Class<?>> allInterfaces = interfacesPerClass.stream().flatMap(Set::stream).collect(Collectors.toSet());
			interfacesPerClass
				.forEach(setOfInterfaces -> allInterfaces.removeIf(iface -> !setOfInterfaces.contains(iface)));
			candidate = (allInterfaces.size() != 1) ? null : allInterfaces.iterator().next();
		}

		return candidate;
	}

	static PropertyFilter computeIncludePropertyPredicate(Collection<PropertyFilter.ProjectedPath> includedProperties,
			NodeDescription<?> nodeDescription) {

		return PropertyFilter.from(includedProperties, nodeDescription);
	}

	static void updateVersionPropertyIfPossible(Neo4jPersistentEntity<?> entityMetaData,
			PersistentPropertyAccessor<?> propertyAccessor, Entity newOrUpdatedNode) {
		if (entityMetaData.hasVersionProperty()) {
			var versionProperty = entityMetaData.getRequiredVersionProperty();
			propertyAccessor.setProperty(versionProperty,
					newOrUpdatedNode.get(versionProperty.getPropertyName()).asLong());
		}
	}

	/**
	 * Merges statement and explicit parameters. Statement parameters have a higher
	 * precedence
	 * @param statement a statement that maybe has some stored parameters
	 * @param parameters the original parameters
	 * @return the merged parameters
	 */
	static Map<String, Object> mergeParameters(Statement statement, Map<String, Object> parameters) {

		Map<String, Object> mergedParameters = new HashMap<>(statement.getCatalog().getParameters());
		if (parameters != null) {
			mergedParameters.putAll(parameters);
		}
		return mergedParameters;
	}

	/**
	 * Checks if the {@code domainType} is a known entity in the {@code mappingContext}
	 * and retrieves the mapping function for it. If the {@code resultType} is not an
	 * interface, a DTO based projection further down the chain is assumed and therefore a
	 * call to {@link EntityInstanceWithSource#decorateMappingFunction(BiFunction)} is
	 * made, so that a
	 * {@link org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter} can
	 * be used with the query result.
	 * @param mappingContext needed for retrieving the original mapping function
	 * @param domainType the actual domain type (a
	 * {@link org.springframework.data.neo4j.core.schema.Node}).
	 * @param resultType an optional different result type
	 * @param <T> the domain type
	 * @return a mapping function
	 */
	static <T> Supplier<BiFunction<TypeSystem, MapAccessor, ?>> getAndDecorateMappingFunction(
			Neo4jMappingContext mappingContext, Class<T> domainType, @Nullable Class<?> resultType) {

		Assert.notNull(mappingContext.getPersistentEntity(domainType), "Cannot get or create persistent entity");
		return () -> {
			BiFunction<TypeSystem, MapAccessor, ?> mappingFunction = mappingContext
				.getRequiredMappingFunctionFor(domainType);
			if (resultType != null && domainType != resultType && !resultType.isInterface()) {
				mappingFunction = EntityInstanceWithSource.decorateMappingFunction(mappingFunction);
			}
			return mappingFunction;
		};
	}

	/**
	 * Computes a {@link PropertyFilter} from a set of included properties based on an
	 * entities meta data and applies it to a given binder function.
	 * @param includedProperties the set of included properties
	 * @param entityMetaData the metadata of the entity in question
	 * @param binderFunction the original binder function for persisting the entity.
	 * @param <T> the type of the entity
	 * @return a new binder function that only works on the included properties.
	 */
	static <T> FilteredBinderFunction<T> createAndApplyPropertyFilter(
			Collection<PropertyFilter.ProjectedPath> includedProperties, Neo4jPersistentEntity<?> entityMetaData,
			Function<T, Map<String, Object>> binderFunction) {

		PropertyFilter includeProperty = TemplateSupport.computeIncludePropertyPredicate(includedProperties,
				entityMetaData);
		return new FilteredBinderFunction<>(includeProperty, binderFunction.andThen(tree -> {
			@SuppressWarnings("unchecked")
			Map<String, Object> properties = (Map<String, Object>) tree.get(Constants.NAME_OF_PROPERTIES_PARAM);

			String idPropertyName = entityMetaData.getRequiredIdProperty().getPropertyName();
			IdDescription idDescription = entityMetaData.getIdDescription();
			boolean assignedId = idDescription != null
					&& (idDescription.isAssignedId() || idDescription.isExternallyGeneratedId());
			if (!(includeProperty.isNotFiltering() || properties == null)) {
				properties.entrySet().removeIf(e -> {
					// we cannot skip the id property if it is an assigned id
					boolean isIdProperty = e.getKey().equals(idPropertyName);
					return !(assignedId && isIdProperty)
							&& !includeProperty.contains(e.getKey(), entityMetaData.getUnderlyingClass());
				});
			}
			return tree;
		}));
	}

	/**
	 * Helper function that computes the map of included properties for a dynamic
	 * projection as expected in 6.2, but for fully dynamic projection.
	 * @param mappingContext the context to work on
	 * @param domainType the projected domain type
	 * @param predicate the predicate to compute the included columns
	 * @param <T> the type of the domain type
	 * @return a map as expected by the property filter.
	 */
	static <T> Collection<PropertyFilter.ProjectedPath> computeIncludedPropertiesFromPredicate(
			Neo4jMappingContext mappingContext, Class<T> domainType,
			BiPredicate<PropertyPath, Neo4jPersistentProperty> predicate) {
		if (predicate == null) {
			return Collections.emptySet();
		}
		Collection<PropertyFilter.ProjectedPath> pps = new HashSet<>();
		PropertyTraverser traverser = new PropertyTraverser(mappingContext);
		traverser
			.traverse(domainType, predicate,
					(path, property) -> pps.add(new PropertyFilter.ProjectedPath(
							PropertyFilter.RelaxedPropertyPath.withRootType(domainType).append(path.toDotPath()),
							false)));
		return pps;
	}

	/**
	 * Uses the given {@link PersistentPropertyAccessor propertyAccessor} to set the value
	 * of the generated id.
	 * @param entityMetaData the type information from SDN
	 * @param propertyAccessor an accessor tied to a concrete instance
	 * @param elementId the element id to store
	 * @param databaseEntity a fallback entity to retrieve the deprecated internal long id
	 * @param <T> the type of the entity
	 */
	@SuppressWarnings("deprecation")
	static <T> void setGeneratedIdIfNecessary(Neo4jPersistentEntity<?> entityMetaData,
			PersistentPropertyAccessor<T> propertyAccessor, Object elementId, Optional<Entity> databaseEntity) {
		if (!entityMetaData.isUsingInternalIds()) {
			return;
		}
		var requiredIdProperty = entityMetaData.getRequiredIdProperty();
		var idPropertyType = requiredIdProperty.getType();
		if (entityMetaData.isUsingDeprecatedInternalId()) {
			propertyAccessor.setProperty(requiredIdProperty,
					databaseEntity.map(IdentitySupport::getInternalId).orElseThrow());
		}
		else if (idPropertyType.equals(String.class)) {
			propertyAccessor.setProperty(requiredIdProperty, elementId);
		}
		else {
			throw new IllegalArgumentException("Unsupported generated id property " + idPropertyType);
		}
	}

	/**
	 * Retrieves the object id for a related object if no has been found so far or updates
	 * the object with the id of a previously processed object.
	 * @param entityMetadata needed for determining the type of ids
	 * @param propertyAccessor bound to the currently processed entity
	 * @param databaseEntity source for the old neo4j internal id
	 * @param relatedInternalId the element id or the string version of the old id
	 * @param <T> the type of the entity
	 * @return the actual related internal id being used.
	 */
	@SuppressWarnings("deprecation")
	static <T> Object retrieveOrSetRelatedId(Neo4jPersistentEntity<?> entityMetadata,
			PersistentPropertyAccessor<T> propertyAccessor,
			@SuppressWarnings("OptionalUsedAsFieldOrParameterType") Optional<Entity> databaseEntity,
			@Nullable Object relatedInternalId) {
		if (!entityMetadata.isUsingInternalIds()) {
			return Objects.requireNonNull(relatedInternalId);
		}

		var requiredIdProperty = entityMetadata.getRequiredIdProperty();
		var current = propertyAccessor.getProperty(requiredIdProperty);

		if (entityMetadata.isUsingDeprecatedInternalId()) {
			if (relatedInternalId == null && current != null) {
				relatedInternalId = current.toString();
			}
			else if (current == null) {
				long internalId = databaseEntity.map(Entity::id).orElseThrow();
				propertyAccessor.setProperty(requiredIdProperty, internalId);
			}
		}
		else {
			if (relatedInternalId == null && current != null) {
				relatedInternalId = current;
			}
			else if (current == null) {
				propertyAccessor.setProperty(requiredIdProperty, relatedInternalId);
			}
		}
		return Objects.requireNonNull(relatedInternalId);
	}

	/**
	 * Checks if the renderer is configured in such a way that it will use element id or
	 * apply toString(id(n)) workaround.
	 * @param renderer the rendered to check
	 * @param targetEntity the entity that might use internal ids
	 * @return {@literal true} if renderer will use elementId
	 */
	static boolean rendererCanUseElementIdIfPresent(Renderer renderer, Neo4jPersistentEntity<?> targetEntity) {
		return !targetEntity.isUsingDeprecatedInternalId() && rendererRendersElementId(renderer);
	}

	static boolean rendererRendersElementId(Renderer renderer) {
		return renderer.render(Cypher.returning(Cypher.elementId(Cypher.anyNode("n"))).build())
			.equals("RETURN elementId(n)");
	}

	public static String convertIdOrElementIdToString(Object value) {
		if (value instanceof Value driverValue) {
			if (driverValue.hasType(TypeSystem.getDefault().NUMBER())) {
				return driverValue.asNumber().toString();
			}
			return driverValue.asString();
		}

		return value.toString();
	}

	@Nullable static Object convertToLongIdOrStringElementId(@Nullable Collection<String> ids) {
		if (ids == null) {
			return null;
		}
		try {
			return ids.stream().map(Long::valueOf).collect(Collectors.toSet());

		}
		catch (Exception ex) {
			return ids;
		}
	}

	static Object convertIdValues(Neo4jMappingContext ctx, @Nullable Neo4jPersistentProperty idProperty,
			@Nullable Object idValues) {

		if (idProperty != null && ((Neo4jPersistentEntity<?>) idProperty.getOwner()).isUsingInternalIds()) {
			return (idValues != null) ? idValues : Values.NULL;
		}

		if (idValues != null) {
			return ctx.getConversionService()
				.writeValue(idValues, TypeInformation.of(idValues.getClass()),
						(idProperty != null) ? idProperty.getOptionalConverter() : null);
		}
		else if (idProperty != null) {
			return ctx.getConversionService()
				.writeValue(idValues, idProperty.getTypeInformation(), idProperty.getOptionalConverter());
		}
		else {
			// Not much we can convert here
			return Values.NULL;
		}
	}

	enum FetchType {

		ONE, ALL

	}

	/**
	 * Indicator for an empty collection.
	 */
	public static final class EmptyIterable {

		private EmptyIterable() {
		}

	}

	/**
	 * Parameter holder class for a query with the return pattern of `rootNodes,
	 * relationships, relatedNodes`. The parameter values must be internal node or
	 * relationship ids.
	 */
	static final class NodesAndRelationshipsByIdStatementProvider {

		static final NodesAndRelationshipsByIdStatementProvider EMPTY = new NodesAndRelationshipsByIdStatementProvider(
				Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), new QueryFragments(),
				SpringDataCypherDsl.elementIdOrIdFunction.apply(Dialect.NEO4J_4));

		private static final String ROOT_NODE_IDS = "rootNodeIds";

		private static final String RELATIONSHIP_IDS = "relationshipIds";

		private static final String RELATED_NODE_IDS = "relatedNodeIds";

		private final Map<String, Collection<String>> parameters = new HashMap<>(3);

		private final QueryFragments queryFragments;

		private final Function<Named, FunctionInvocation> elementIdFunction;

		NodesAndRelationshipsByIdStatementProvider(Collection<String> rootNodeIds, Collection<String> relationshipsIds,
				Collection<String> relatedNodeIds, QueryFragments queryFragments,
				Function<Named, FunctionInvocation> elementIdFunction) {

			this.elementIdFunction = elementIdFunction;
			this.parameters.put(ROOT_NODE_IDS, rootNodeIds);
			this.parameters.put(RELATIONSHIP_IDS, relationshipsIds);
			this.parameters.put(RELATED_NODE_IDS, relatedNodeIds);
			this.queryFragments = queryFragments;

		}

		boolean hasRootNodeIds() {
			var ids = this.parameters.get(ROOT_NODE_IDS);
			return ids != null && !ids.isEmpty();
		}

		Statement toStatement(NodeDescription<?> nodeDescription) {

			String primaryLabel = nodeDescription.getPrimaryLabel();
			Node rootNodes = Cypher.node(primaryLabel).named(ROOT_NODE_IDS);
			Node relatedNodes = Cypher.anyNode(RELATED_NODE_IDS);

			List<Expression> projection = new ArrayList<>();
			projection.add(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription)
				.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
			projection.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS));
			projection.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
			projection.addAll(this.queryFragments.getAdditionalReturnExpressions());

			Relationship relationships = Cypher.anyNode().relationshipBetween(Cypher.anyNode()).named(RELATIONSHIP_IDS);
			return Cypher.match(rootNodes)
				.where(this.elementIdFunction.apply(rootNodes)
					.in(Cypher.parameter(ROOT_NODE_IDS,
							convertToLongIdOrStringElementId(this.parameters.get(ROOT_NODE_IDS)))))
				.with(Cypher.collect(rootNodes).as(Constants.NAME_OF_ROOT_NODE))
				.optionalMatch(relationships)
				.where(this.elementIdFunction.apply(relationships)
					.in(Cypher.parameter(RELATIONSHIP_IDS,
							convertToLongIdOrStringElementId(this.parameters.get(RELATIONSHIP_IDS)))))
				.with(Constants.NAME_OF_ROOT_NODE,
						Cypher.collectDistinct(relationships).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS))
				.optionalMatch(relatedNodes)
				.where(this.elementIdFunction.apply(relatedNodes)
					.in(Cypher.parameter(RELATED_NODE_IDS,
							convertToLongIdOrStringElementId(this.parameters.get(RELATED_NODE_IDS)))))
				.with(Constants.NAME_OF_ROOT_NODE,
						Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS)
							.as(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
						Cypher.collectDistinct(relatedNodes).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES))
				.unwind(Constants.NAME_OF_ROOT_NODE)
				.as(ROOT_NODE_IDS)
				.with(Cypher.name(ROOT_NODE_IDS)
					.as(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription).getValue()),
						Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
						Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES))
				.orderBy(this.queryFragments.getOrderBy())
				.returning(projection)
				.skip(this.queryFragments.getSkip())
				.limit(this.queryFragments.getLimit())
				.build();
		}

	}

	/**
	 * A wrapper around a {@link Function} from entity to {@link Map} which is filtered
	 * the {@link PropertyFilter} included as well.
	 *
	 * @param <T> the type of the entity
	 */
	static class FilteredBinderFunction<T> implements Function<T, Map<String, Object>> {

		final PropertyFilter filter;

		final Function<T, Map<String, Object>> binderFunction;

		FilteredBinderFunction(PropertyFilter filter, Function<T, Map<String, Object>> binderFunction) {
			this.filter = filter;
			this.binderFunction = binderFunction;
		}

		@Override
		public Map<String, Object> apply(T t) {
			return this.binderFunction.apply(t);
		}

	}

}
