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
package org.springframework.data.neo4j.core;

import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.EntityInstanceWithSource;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.repository.query.MagicPropertyPathClass;
import org.springframework.data.neo4j.repository.query.QueryFragments;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.mapping.MappingSupport;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Utilities for templates.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Ride The Lightning
 * @since 6.0.9
 */
@API(status = API.Status.INTERNAL, since = "6.0.9")
final class TemplateSupport {

	enum FetchType {

		ONE,
		ALL
	}

	@Nullable
	static Class<?> findCommonElementType(Iterable<?> collection) {

		List<Class<?>> allClasses = StreamSupport.stream(collection.spliterator(), true)
				.filter(o -> o != null)
				.map(Object::getClass).collect(Collectors.toList());

		Class<?> candidate = null;
		for (Class<?> type : allClasses) {
			if (candidate == null) {
				candidate = type;
			} else if (candidate != type) {
				candidate = null;
				break;
			}
		}

		if (candidate != null) {
			return candidate;
		} else {
			Predicate<Class<?>> moveUp = c -> c != null && c != Object.class;
			Set<Class<?>> mostAbstractClasses = new HashSet<>();
			for (Class<?> type : allClasses) {
				while (moveUp.test(type.getSuperclass())) {
					type = type.getSuperclass();
				}
				mostAbstractClasses.add(type);
			}
			candidate = mostAbstractClasses.size() == 1 ? mostAbstractClasses.iterator().next() : null;
		}

		if (candidate != null) {
			return candidate;
		} else {
			List<Set<Class<?>>> interfacesPerClass = allClasses.stream()
					.map(c -> Arrays.stream(c.getInterfaces()).collect(Collectors.toSet()))
					.collect(Collectors.toList());
			Set<Class<?>> allInterfaces = interfacesPerClass.stream().flatMap(Set::stream).collect(Collectors.toSet());
			interfacesPerClass
					.forEach(setOfInterfaces -> allInterfaces.removeIf(iface -> !setOfInterfaces.contains(iface)));
			candidate = allInterfaces.size() == 1 ? allInterfaces.iterator().next() : null;
		}

		return candidate;
	}

	static MagicPropertyPathClass computeIncludePropertyPredicate(Collection<PropertyPath> includedProperties,
																  NodeDescription<?> nodeDescription) {

		return MagicPropertyPathClass.from(includedProperties, nodeDescription);
	}

	static void updateVersionPropertyIfPossible(
			Neo4jPersistentEntity<?> entityMetaData,
			PersistentPropertyAccessor<?> propertyAccessor,
			Entity newOrUpdatedNode
	) {
		if (entityMetaData.hasVersionProperty()) {
			propertyAccessor.setProperty(
					entityMetaData.getVersionProperty(), newOrUpdatedNode.get(entityMetaData.getVersionProperty().getPropertyName()).asLong());
		}
	}

	/**
	 * Merges statement and explicit parameters. Statement parameters have a higher precedence
	 *
	 * @param statement  A statement that maybe has some stored parameters
	 * @param parameters The original parameters
	 * @return Merged parameters
	 */
	static Map<String, Object> mergeParameters(Statement statement, @Nullable Map<String, Object> parameters) {

		Map<String, Object> mergedParameters = new HashMap<>(statement.getParameters());
		if (parameters != null) {
			mergedParameters.putAll(parameters);
		}
		return mergedParameters;
	}

	/**
	 * Parameter holder class for a query with the return pattern of `rootNodes, relationships, relatedNodes`.
	 * The parameter values must be internal node or relationship ids.
	 */
	static final class NodesAndRelationshipsByIdStatementProvider {

		private final static String ROOT_NODE_IDS = "rootNodeIds";
		private final static String RELATIONSHIP_IDS = "relationshipIds";
		private final static String RELATED_NODE_IDS = "relatedNodeIds";

		final static NodesAndRelationshipsByIdStatementProvider EMPTY =
				new NodesAndRelationshipsByIdStatementProvider(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), new QueryFragments());

		private final Map<String, Collection<Long>> parameters = new HashMap<>(3);
		private final QueryFragments queryFragments;

		NodesAndRelationshipsByIdStatementProvider(Collection<Long> rootNodeIds, Collection<Long> relationshipsIds, Collection<Long> relatedNodeIds, QueryFragments queryFragments) {

			this.parameters.put(ROOT_NODE_IDS, rootNodeIds);
			this.parameters.put(RELATIONSHIP_IDS, relationshipsIds);
			this.parameters.put(RELATED_NODE_IDS, relatedNodeIds);
			this.queryFragments = queryFragments;
		}

		Map<String, Object> getParameters() {
			return Collections.unmodifiableMap(parameters);
		}

		boolean hasRootNodeIds() {
			return parameters.get(ROOT_NODE_IDS).isEmpty();
		}

		Statement toStatement() {

			String rootNodeIds = "rootNodeIds";
			String relationshipIds = "relationshipIds";
			String relatedNodeIds = "relatedNodeIds";
			Node rootNodes = Cypher.anyNode(rootNodeIds);
			Node relatedNodes = Cypher.anyNode(relatedNodeIds);
			Relationship relationships = Cypher.anyNode().relationshipBetween(Cypher.anyNode()).named(relationshipIds);
			return Cypher.match(rootNodes)
					.where(Functions.id(rootNodes).in(Cypher.parameter(rootNodeIds)))
					.optionalMatch(relationships)
					.where(Functions.id(relationships).in(Cypher.parameter(relationshipIds)))
					.optionalMatch(relatedNodes)
					.where(Functions.id(relatedNodes).in(Cypher.parameter(relatedNodeIds)))
					.with(
							rootNodes.as(Constants.NAME_OF_ROOT_NODE.getValue()),
							Functions.collectDistinct(relationships).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
							Functions.collectDistinct(relatedNodes).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES))
					.orderBy(queryFragments.getOrderBy())
					.returning(
							Constants.NAME_OF_ROOT_NODE.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE),
							Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
							Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES)
					)
					.skip(queryFragments.getSkip())
					.limit(queryFragments.getLimit()).build();
		}
	}

	/**
	 * Checks if the {@code domainType} is a known entity in the {@code mappingContext} and retrieves the mapping function
	 * for it. If the {@code resultType} is not an interface, a DTO based projection further down the chain is assumed
	 * and therefor a call to {@link EntityInstanceWithSource#decorateMappingFunction(BiFunction)} is made, so that
	 * a {@link org.springframework.data.neo4j.core.mapping.DtoInstantiatingConverter} can be used with the query result.
	 *
	 * @param mappingContext Needed for retrieving the original mapping function
	 * @param domainType     The actual domain type (a {@link org.springframework.data.neo4j.core.schema.Node}.
	 * @param resultType     An optional different result type
	 * @param <T>            The domain type
	 * @return A mapping function
	 */
	static <T> BiFunction<TypeSystem, MapAccessor, ?> getAndDecorateMappingFunction(
			Neo4jMappingContext mappingContext, Class<T> domainType, @Nullable Class<?> resultType) {

		Assert.notNull(mappingContext.getPersistentEntity(domainType), "Cannot get or create persistent entity.");
		BiFunction<TypeSystem, MapAccessor, ?> mappingFunction = mappingContext
				.getRequiredMappingFunctionFor(domainType);
		if (resultType != null && domainType != resultType && !resultType.isInterface()) {
			mappingFunction = EntityInstanceWithSource.decorateMappingFunction(mappingFunction);
		}
		return mappingFunction;
	}

	private TemplateSupport() {
	}
}
