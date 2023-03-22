/*
 * Copyright 2011-2023 the original author or authors.
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

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.RelationshipPattern;
import org.neo4j.cypherdsl.core.SortItem;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.neo4j.cypherdsl.core.Cypher.parameter;

/**
 * Combines the QueryFragments with parameters.
 *
 * @author Gerrit Meier
 * @since 6.0.4
 */
@API(status = API.Status.INTERNAL, since = "6.0.4")
public final class QueryFragmentsAndParameters {
	private final static CypherGenerator cypherGenerator = CypherGenerator.INSTANCE;
	private Map<String, Object> parameters;
	private NodeDescription<?> nodeDescription;
	private final QueryFragments queryFragments;
	private final String cypherQuery;
	private final Sort sort;

	public QueryFragmentsAndParameters(NodeDescription<?> nodeDescription, QueryFragments queryFragments,
									   @Nullable Map<String, Object> parameters, @Nullable Sort sort) {
		this.nodeDescription = nodeDescription;
		this.queryFragments = queryFragments;
		this.parameters = parameters;
		this.cypherQuery = null;
		this.sort = sort == null ? Sort.unsorted() : sort;
	}

	public QueryFragmentsAndParameters(String cypherQuery) {
		this(cypherQuery, null);
	}

	public QueryFragmentsAndParameters(String cypherQuery, @Nullable Map<String, Object> parameters) {
		this.cypherQuery = cypherQuery;
		this.queryFragments = new QueryFragments();
		this.parameters = parameters;
		this.sort = Sort.unsorted();
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public QueryFragments getQueryFragments() {
		return queryFragments;
	}

	public String getCypherQuery() {
		return cypherQuery;
	}

	public NodeDescription<?> getNodeDescription() {
		return nodeDescription;
	}

	public void setParameters(Map<String, Object> newParameters) {
		this.parameters = newParameters;
	}

	public Sort getSort() {
		return sort;
	}

	/*
	 * Convenience methods that are used by the (Reactive)Neo4jTemplate
	 */
	public static QueryFragmentsAndParameters forFindById(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_ID, idValues);

		Node container = cypherGenerator.createRootNode(entityMetaData);
		Condition condition;
		if (entityMetaData.getIdProperty().isComposite()) {
			condition = CypherGenerator.INSTANCE.createCompositePropertyCondition(entityMetaData.getIdProperty(), container.getRequiredSymbolicName(), parameter(Constants.NAME_OF_ID));
		} else {
			condition = entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID));
		}

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(container);
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters, null);
	}

	public static QueryFragmentsAndParameters forFindByAllId(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_IDS, idValues);

		Node container = cypherGenerator.createRootNode(entityMetaData);
		Condition condition;
		if (entityMetaData.getIdProperty().isComposite()) {
			List<Object> args = new ArrayList<>();
			for (String key : entityMetaData.getIdProperty().getOptionalConverter().write(null).keys()) {
				args.add(key);
				args.add(container.property(key));
			}
			condition = Cypher.mapOf(args.toArray()).in(parameter(Constants.NAME_OF_IDS));
		} else {
			condition = entityMetaData.getIdExpression().in(parameter(Constants.NAME_OF_IDS));
		}

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(container);
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters, null);
	}

	public static QueryFragmentsAndParameters forFindAll(Neo4jPersistentEntity<?> entityMetaData) {
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(Conditions.noCondition());
		queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, Collections.emptyMap(), null);
	}

	public static QueryFragmentsAndParameters forExistsById(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_ID, idValues);

		Node container = cypherGenerator.createRootNode(entityMetaData);
		Condition condition;
		if (entityMetaData.getIdProperty().isComposite()) {
			condition = CypherGenerator.INSTANCE.createCompositePropertyCondition(entityMetaData.getIdProperty(), container.getRequiredSymbolicName(), parameter(Constants.NAME_OF_ID));
		} else {
			condition = entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID));
		}

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(container);
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForExists(entityMetaData));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters, null);
	}

	/*
	 * Following methods are used by the Simple(Reactive)QueryByExampleExecutor
	 */
	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example,
				(java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath>) null);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, @Nullable java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, null, null, includeField);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, Sort sort) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, sort, null);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, Sort sort, @Nullable java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, null, sort, includeField);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, Pageable pageable) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, pageable, null);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, Pageable pageable, @Nullable java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, pageable, null, includeField);
	}

	/**
	 * Utility method for creating a query fragment including parameters for a given condition.
	 *
	 * @param entityMetaData The metadata of a given and known entity
	 * @param condition      A Cypher-DSL condition
	 * @return Fully populated fragments and parameter
	 */
	@API(status = API.Status.EXPERIMENTAL, since = "6.1.7")
	public static QueryFragmentsAndParameters forCondition(Neo4jPersistentEntity<?> entityMetaData, Condition condition) {
		return forCondition(entityMetaData, condition, null, null);
	}

	static QueryFragmentsAndParameters forCondition(Neo4jPersistentEntity<?> entityMetaData,
													Condition condition,
													@Nullable Pageable pageable,
													@Nullable Collection<SortItem> sortItems
	) {
		return forCondition(entityMetaData, condition, pageable, sortItems, null);
	}

	static QueryFragmentsAndParameters forCondition(Neo4jPersistentEntity<?> entityMetaData,
													Condition condition,
													@Nullable Pageable pageable,
													@Nullable Collection<SortItem> sortItems,
													@Nullable java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField
	) {

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		if (includeField == null) {
			queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		} else {
			queryFragments.setReturnExpressions(
					cypherGenerator.createReturnStatementForMatch(entityMetaData, includeField));
		}
		queryFragments.setRenderConstantsAsParameters(true);

		if (pageable != null) {
			adaptPageable(entityMetaData, pageable, queryFragments);
		} else if (sortItems != null) {
			queryFragments.setOrderBy(sortItems);
		}

		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, Collections.emptyMap(), null);
	}

	private static void adaptPageable(
			Neo4jPersistentEntity<?> entityMetaData,
			Pageable pageable,
			QueryFragments queryFragments
	) {
		Sort pageableSort = pageable.getSort();
		queryFragments.setSkip(pageable.getOffset());
		queryFragments.setLimit(pageable.getPageSize());
		queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, pageableSort));
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example,
												  @Nullable Pageable pageable, @Nullable Sort sort, java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Map<String, Object> parameters = predicate.getParameters();
		Set<PropertyPathWrapper> propertyPathWrappers = predicate.getPropertyPathWrappers();
		Condition condition = predicate.getCondition();

		return getQueryFragmentsAndParameters(mappingContext.getPersistentEntity(example.getProbeType()), pageable,
				sort, parameters, condition, includeField, propertyPathWrappers);
	}

	public static QueryFragmentsAndParameters forPageableAndSort(Neo4jPersistentEntity<?> neo4jPersistentEntity,
																 @Nullable Pageable pageable, @Nullable Sort sort) {

		return getQueryFragmentsAndParameters(neo4jPersistentEntity, pageable, sort, Collections.emptyMap(), null, null, null);
	}

	private static QueryFragmentsAndParameters getQueryFragmentsAndParameters(
			Neo4jPersistentEntity<?> entityMetaData, @Nullable Pageable pageable, @Nullable Sort sort,
			@Nullable Map<String, Object> parameters, @Nullable Condition condition, @Nullable
			java.util.function.Predicate<PropertyFilter.RelaxedPropertyPath> includeField,
			@Nullable Set<PropertyPathWrapper> propertyPathWrappers) {

		QueryFragments queryFragments = new QueryFragments();

		if (propertyPathWrappers != null && !propertyPathWrappers.isEmpty()) {
			Node startNode = Cypher.node(entityMetaData.getPrimaryLabel(), entityMetaData.getAdditionalLabels())
					.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(entityMetaData));
			List<PatternElement> relationshipChain = new ArrayList<>();
			for (PropertyPathWrapper possiblePathWithRelationship : propertyPathWrappers) {
				relationshipChain.add((RelationshipPattern) possiblePathWithRelationship.createRelationshipChain(startNode));
			}
			queryFragments.setMatchOn(relationshipChain);
		} else {
			queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		}
		queryFragments.setCondition(condition);
		if (includeField == null) {
			queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		} else {
			queryFragments.setReturnExpressions(
					cypherGenerator.createReturnStatementForMatch(entityMetaData, includeField));
		}

		if (pageable != null) {
			adaptPageable(entityMetaData, pageable, queryFragments);
		} else if (sort != null) {
			queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, sort));
		}

		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters, sort);
	}

}
