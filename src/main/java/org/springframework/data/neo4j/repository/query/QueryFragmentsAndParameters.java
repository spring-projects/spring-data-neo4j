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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.RelationshipPattern;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.PropertyFilterSupport;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;

import static org.neo4j.cypherdsl.core.Cypher.parameter;

/**
 * Combines the QueryFragments with parameters.
 *
 * @author Gerrit Meier
 * @since 6.0.4
 */
@API(status = API.Status.INTERNAL, since = "6.0.4")
public final class QueryFragmentsAndParameters {

	private static final CypherGenerator cypherGenerator = CypherGenerator.INSTANCE;

	private final QueryFragments queryFragments;

	@Nullable
	private final VectorSearchFragment vectorSearchFragment;

	@Nullable
	private final String cypherQuery;

	private final Sort sort;

	private Map<String, Object> parameters;

	@Nullable
	private NodeDescription<?> nodeDescription;

	public QueryFragmentsAndParameters(@Nullable NodeDescription<?> nodeDescription, QueryFragments queryFragments,
			@Nullable VectorSearchFragment vectorSearchFragment, Map<String, Object> parameters, @Nullable Sort sort) {
		this.nodeDescription = nodeDescription;
		this.queryFragments = queryFragments;
		this.vectorSearchFragment = vectorSearchFragment;
		this.parameters = parameters;
		this.cypherQuery = null;
		this.sort = (sort != null) ? sort : Sort.unsorted();
	}

	public QueryFragmentsAndParameters(@Nullable NodeDescription<?> nodeDescription, QueryFragments queryFragments,
			Map<String, Object> parameters, @Nullable Sort sort) {
		this.nodeDescription = nodeDescription;
		this.queryFragments = queryFragments;
		this.vectorSearchFragment = null;
		this.parameters = parameters;
		this.cypherQuery = null;
		this.sort = (sort != null) ? sort : Sort.unsorted();
	}

	public QueryFragmentsAndParameters(@NonNull String cypherQuery) {
		this(cypherQuery, Map.of());
	}

	public QueryFragmentsAndParameters(@NonNull String cypherQuery, Map<String, Object> parameters) {
		this.cypherQuery = cypherQuery;
		this.queryFragments = new QueryFragments();
		this.vectorSearchFragment = null;
		this.parameters = parameters;
		this.sort = Sort.unsorted();
	}

	/*
	 * Convenience methods that are used by the (Reactive)Neo4jTemplate
	 */
	public static QueryFragmentsAndParameters forFindById(Neo4jPersistentEntity<?> entityMetaData, Object idValues,
			Neo4jMappingContext mappingContext) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_ID, idValues);

		QueryFragments queryFragments = forFindOrExistsById(entityMetaData);
		queryFragments
			.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData, PropertyFilterSupport
				.createRelaxedPropertyPathFilter(entityMetaData.getUnderlyingClass(), mappingContext)));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters, null);
	}

	private static QueryFragments forFindOrExistsById(Neo4jPersistentEntity<?> entityMetaData) {
		Node container = cypherGenerator.createRootNode(entityMetaData);
		Condition condition;
		var idProperty = entityMetaData.getIdProperty();
		if (idProperty != null && idProperty.isComposite()) {
			condition = CypherGenerator.INSTANCE.createCompositePropertyCondition(idProperty,
					container.getRequiredSymbolicName(), parameter(Constants.NAME_OF_ID));
		}
		else {
			condition = entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID));
		}

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(container);
		queryFragments.setCondition(condition);
		return queryFragments;
	}

	public static QueryFragmentsAndParameters forFindByAllId(Neo4jPersistentEntity<?> entityMetaData, Object idValues,
			Neo4jMappingContext mappingContext) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_IDS, idValues);

		Node container = cypherGenerator.createRootNode(entityMetaData);
		Condition condition;
		var idProperty = entityMetaData.getIdProperty();
		if (idProperty != null && idProperty.isComposite()) {
			List<Object> args = new ArrayList<>();
			for (String key : Objects.requireNonNull(idProperty.getOptionalConverter()).write(null).keys()) {
				args.add(key);
				args.add(container.property(key));
			}
			condition = Cypher.mapOf(args.toArray()).in(parameter(Constants.NAME_OF_IDS));
		}
		else {
			condition = entityMetaData.getIdExpression().in(parameter(Constants.NAME_OF_IDS));
		}

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(container);
		queryFragments.setCondition(condition);
		queryFragments
			.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData, PropertyFilterSupport
				.createRelaxedPropertyPathFilter(entityMetaData.getUnderlyingClass(), mappingContext)));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters, null);
	}

	public static QueryFragmentsAndParameters forFindAll(Neo4jPersistentEntity<?> entityMetaData,
			Neo4jMappingContext mappingContext) {
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(Cypher.noCondition());
		queryFragments
			.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData, PropertyFilterSupport
				.createRelaxedPropertyPathFilter(entityMetaData.getUnderlyingClass(), mappingContext)));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, Map.of(), null);
	}

	public static QueryFragmentsAndParameters forExistsById(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_ID, idValues);

		QueryFragments queryFragments = forFindOrExistsById(entityMetaData);
		queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForExists(entityMetaData));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments,
				Objects.requireNonNullElseGet(parameters, Map::of), null);
	}

	public static QueryFragmentsAndParameters forPageableAndSort(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			@Nullable Pageable pageable, @Nullable Sort sort) {

		return getQueryFragmentsAndParameters(neo4jPersistentEntity, pageable, sort, null, null, null,
				Collections.emptyMap(), null, null, null);
	}

	/*
	 * Following methods are used by the Simple(Reactive)QueryByExampleExecutor
	 */
	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example) {
		return forExample(mappingContext, example, null, null, null, null, null, null, null);
	}

	static QueryFragmentsAndParameters forExampleWithPageable(Neo4jMappingContext mappingContext, Example<?> example,
			Pageable pageable, java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return forExample(mappingContext, example, null, pageable, null, null, null, null, includeField);
	}

	static QueryFragmentsAndParameters forExampleWithSort(Neo4jMappingContext mappingContext, Example<?> example,
			Sort sort, @Nullable Integer limit,
			java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return forExample(mappingContext, example, null, null, sort, limit, null, null, includeField);
	}

	static QueryFragmentsAndParameters forExampleWithScrollPosition(Neo4jMappingContext mappingContext,
			Example<?> example, @Nullable Condition keysetScrollPositionCondition, Sort sort, Integer limit, Long skip,
			ScrollPosition scrollPosition,
			java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return forExample(mappingContext, example, keysetScrollPositionCondition, null, sort, limit, skip,
				scrollPosition, includeField);
	}

	private static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example,
			@Nullable Condition keysetScrollPositionCondition, @Nullable Pageable pageable, @Nullable Sort sort,
			@Nullable Integer limit, @Nullable Long skip, @Nullable ScrollPosition scrollPosition,
			@Nullable Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		var predicate = org.springframework.data.neo4j.repository.query.Predicate.create(mappingContext, example);
		Map<String, Object> parameters = predicate.getParameters();
		Set<PropertyPathWrapper> propertyPathWrappers = predicate.getPropertyPathWrappers();
		Condition condition = predicate.getCondition();
		Neo4jPersistentEntity<?> persistentEntity = Objects.requireNonNull(
				mappingContext.getPersistentEntity(example.getProbeType()),
				() -> "Could not load persistent entity for probe type %s".formatted(example.getProbeType()));
		if (scrollPosition instanceof KeysetScrollPosition keysetScrollPosition) {

			if (!keysetScrollPosition.isInitial()) {
				condition = condition.and(keysetScrollPositionCondition);
			}
			QueryFragmentsAndParameters queryFragmentsAndParameters = getQueryFragmentsAndParameters(persistentEntity,
					pageable, sort, null, limit, skip, parameters, condition, includeField, propertyPathWrappers);
			queryFragmentsAndParameters.getQueryFragments()
				.setRequiresReverseSort(keysetScrollPosition.scrollsBackward());
			return queryFragmentsAndParameters;
		}

		return getQueryFragmentsAndParameters(persistentEntity, pageable, sort, null, limit, skip, parameters,
				condition, includeField, propertyPathWrappers);
	}

	/**
	 * Utility method for creating a query fragment including parameters for a given
	 * condition.
	 * @param entityMetaData the metadata of a given and known entity
	 * @param condition a Cypher-DSL condition
	 * @return fully populated fragments and parameter
	 */
	@API(status = API.Status.EXPERIMENTAL, since = "6.1.7")
	public static QueryFragmentsAndParameters forCondition(Neo4jPersistentEntity<?> entityMetaData,
			Condition condition) {

		return forCondition(entityMetaData, condition, null, null, null, null, null, null);
	}

	static QueryFragmentsAndParameters forConditionAndPageable(Neo4jPersistentEntity<?> entityMetaData,
			Condition condition, Pageable pageable,
			@Nullable Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		return forCondition(entityMetaData, condition, pageable, null, null, null, null, includeField);
	}

	static QueryFragmentsAndParameters forConditionAndSort(Neo4jPersistentEntity<?> entityMetaData, Condition condition,
			Sort sort, @Nullable Integer limit, @Nullable Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		return forCondition(entityMetaData, condition, null, sort, null, limit, null, includeField);
	}

	static QueryFragmentsAndParameters forConditionAndSortItems(Neo4jPersistentEntity<?> entityMetaData,
			Condition condition, @Nullable Collection<SortItem> sortItems) {
		return forCondition(entityMetaData, condition, null, null, sortItems, null, null, null);
	}

	static QueryFragmentsAndParameters forConditionWithScrollPosition(Neo4jPersistentEntity<?> entityMetaData,
			Condition condition, @Nullable Condition keysetCondition, ScrollPosition scrollPosition, Sort sort,
			@Nullable Integer limit, @Nullable Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		long skip = 0L;

		if (scrollPosition instanceof OffsetScrollPosition offsetScrollPosition) {
			skip = offsetScrollPosition.isInitial() ? 0 : offsetScrollPosition.getOffset() + 1;

			return forCondition(entityMetaData, condition, null, sort, null, limit, skip, includeField);
		}

		if (scrollPosition instanceof KeysetScrollPosition keysetScrollPosition) {
			if (!scrollPosition.isInitial() && keysetCondition != null) {
				condition = condition.and(keysetCondition);
			}
			QueryFragmentsAndParameters queryFragmentsAndParameters = getQueryFragmentsAndParameters(entityMetaData,
					null, sort, null, limit, skip, Collections.emptyMap(), condition, includeField, null);
			queryFragmentsAndParameters.getQueryFragments()
				.setRequiresReverseSort(keysetScrollPosition.scrollsBackward());
			return queryFragmentsAndParameters;
		}

		throw new IllegalArgumentException(
				"ScrollPosition must be of type OffsetScrollPosition or KeysetScrollPosition. Unexpected type %s found."
					.formatted(scrollPosition.getClass()));

	}

	// Parameter re-ordering helper
	private static QueryFragmentsAndParameters forCondition(Neo4jPersistentEntity<?> entityMetaData,
			@Nullable Condition condition, @Nullable Pageable pageable, @Nullable Sort sort,
			@Nullable Collection<SortItem> sortItems, @Nullable Integer limit, @Nullable Long skip,
			@Nullable Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		return getQueryFragmentsAndParameters(entityMetaData, pageable, sort, sortItems, limit, skip,
				Collections.emptyMap(), condition, includeField, null);
	}

	private static QueryFragmentsAndParameters getQueryFragmentsAndParameters(Neo4jPersistentEntity<?> entityMetaData,
			@Nullable Pageable pageable, @Nullable Sort sort, @Nullable Collection<SortItem> sortItems,
			@Nullable Integer limit, @Nullable Long skip, @Nullable Map<String, Object> parameters,
			@Nullable Condition condition, @Nullable Predicate<PropertyFilter.RelaxedPropertyPath> includeField,
			@Nullable Set<PropertyPathWrapper> propertyPathWrappers) {

		QueryFragments queryFragments = new QueryFragments();

		if (propertyPathWrappers != null && !propertyPathWrappers.isEmpty()) {
			Node startNode = Cypher.node(entityMetaData.getPrimaryLabel(), entityMetaData.getAdditionalLabels())
				.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData));
			List<PatternElement> relationshipChain = new ArrayList<>();
			for (PropertyPathWrapper possiblePathWithRelationship : propertyPathWrappers) {
				relationshipChain
					.add((RelationshipPattern) possiblePathWithRelationship.createRelationshipChain(startNode));
			}
			queryFragments.setMatchOn(relationshipChain);
		}
		else {
			queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		}
		queryFragments.setCondition(condition);
		if (includeField == null) {
			queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		}
		else {
			queryFragments
				.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData, includeField));
			queryFragments.setProjectingPropertyFilter(includeField);
		}

		if (pageable != null) {
			adaptPageable(entityMetaData, pageable, queryFragments);
		}
		else {
			if (sort != null) {
				queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, sort));
			}
			else if (sortItems != null) {
				queryFragments.setOrderBy(sortItems);
			}
			if (limit != null) {
				// we don't need to additionally pass the limit to the constructor
				// because it will get fetched from the QueryFragments later
				queryFragments.setLimit(limit);
			}
			if (skip != null) {
				queryFragments.setSkip(skip);
			}
		}

		return new QueryFragmentsAndParameters(entityMetaData, queryFragments,
				Objects.requireNonNullElseGet(parameters, Map::of), sort);
	}

	private static void adaptPageable(Neo4jPersistentEntity<?> entityMetaData, Pageable pageable,
			QueryFragments queryFragments) {
		if (pageable.isPaged()) {
			queryFragments.setSkip(pageable.getOffset());
			queryFragments.setLimit(pageable.getPageSize());
		}
		Sort pageableSort = pageable.getSort();
		if (pageableSort.isSorted()) {
			queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, pageableSort));
		}
	}

	public Map<String, Object> getParameters() {
		return this.parameters;
	}

	public void setParameters(Map<String, Object> newParameters) {
		this.parameters = newParameters;
	}

	public QueryFragments getQueryFragments() {
		return this.queryFragments;
	}

	public boolean hasVectorSearchFragment() {
		return this.vectorSearchFragment != null;
	}

	@Nullable public String getCypherQuery() {
		return this.cypherQuery;
	}

	@Nullable public NodeDescription<?> getNodeDescription() {
		return this.nodeDescription;
	}

	public Sort getSort() {
		return this.sort;
	}

	public Statement toStatement() {
		if (this.hasVectorSearchFragment()) {
			return this.queryFragments.toStatement(Objects.requireNonNull(this.vectorSearchFragment));
		}
		return this.queryFragments.toStatement();
	}

}
