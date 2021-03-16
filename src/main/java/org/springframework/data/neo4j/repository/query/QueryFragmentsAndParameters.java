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
package org.springframework.data.neo4j.repository.query;

import static org.neo4j.cypherdsl.core.Cypher.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.lang.Nullable;

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

	public QueryFragmentsAndParameters(NodeDescription<?> nodeDescription, QueryFragments queryFragments,
									   @Nullable Map<String, Object> parameters) {
		this.nodeDescription = nodeDescription;
		this.queryFragments = queryFragments;
		this.parameters = parameters;
		this.cypherQuery = null;
	}

	public QueryFragmentsAndParameters(String cypherQuery) {
		this.cypherQuery = cypherQuery;
		this.queryFragments = new QueryFragments();
		this.parameters = null;
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

	/*
	 * Convenience methods that are used by the (Reactive)Neo4jTemplate
	 */
	public static QueryFragmentsAndParameters forFindById(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_ID, idValues);

		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID));
		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(returnStatement);
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
	}

	public static QueryFragmentsAndParameters forFindByAllId(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_IDS, idValues);

		Condition condition = entityMetaData.getIdExpression().in((parameter(Constants.NAME_OF_IDS)));
		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(returnStatement);
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
	}

	public static QueryFragmentsAndParameters forFindAll(Neo4jPersistentEntity<?> entityMetaData) {
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(Conditions.noCondition());
		queryFragments.setReturnExpressions(cypherGenerator.createReturnStatementForMatch(entityMetaData));
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, Collections.emptyMap());
	}

	/*
	 * Following methods are used by the Simple(Reactive)QueryByExampleExecutor
	 */
	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, null, null);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, Sort sort) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, null, sort);
	}

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example, Pageable pageable) {
		return QueryFragmentsAndParameters.forExample(mappingContext, example, pageable, null);
	}

	static QueryFragmentsAndParameters forCondition(Neo4jPersistentEntity<?> entityMetaData,
			Condition condition,
			@Nullable Pageable pageable,
			@Nullable SortItem[] sortItems
	) {

		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(returnStatement);
		queryFragments.setRenderConstantsAsParameters(true);

		if (pageable != null) {
			adaptPageable(entityMetaData, pageable, queryFragments);
		} else if (sortItems != null) {
			queryFragments.setOrderBy(sortItems);
		}

		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, Collections.emptyMap());
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
												  @Nullable Pageable pageable, @Nullable Sort sort) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Map<String, Object> parameters = predicate.getParameters();
		Condition condition = predicate.getCondition();

		return getQueryFragmentsAndParameters(mappingContext.getPersistentEntity(example.getProbeType()), pageable,
				sort, parameters, condition);
	}

	public static QueryFragmentsAndParameters forPageableAndSort(Neo4jPersistentEntity<?> neo4jPersistentEntity,
																 @Nullable Pageable pageable, @Nullable Sort sort) {

		return getQueryFragmentsAndParameters(neo4jPersistentEntity, pageable, sort, Collections.emptyMap(), null);
	}

	private static QueryFragmentsAndParameters getQueryFragmentsAndParameters(
			Neo4jPersistentEntity<?> entityMetaData, @Nullable Pageable pageable, @Nullable Sort sort,
			@Nullable Map<String, Object> parameters, @Nullable Condition condition) {

		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(returnStatement);

		if (pageable != null) {
			adaptPageable(entityMetaData, pageable, queryFragments);
		} else if (sort != null) {
			queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, sort));
		}

		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
	}

	/**
	 * Collects the parts of a Cypher query to be handed over to the Cypher generator.
	 *
	 * @author Gerrit Meier
	 * @since 6.0.4
	 */
	@API(status = API.Status.INTERNAL, since = "6.0.4")
	public static final class QueryFragments {
		private List<PatternElement> matchOn = new ArrayList<>();
		private Condition condition;
		private List<Expression> returnExpressions = new ArrayList<>();
		private SortItem[] orderBy;
		private Number limit;
		private Long skip;
		private ReturnTuple returnTuple;
		private boolean scalarValueReturn = false;
		private boolean renderConstantsAsParameters = false;

		public void addMatchOn(PatternElement match) {
			this.matchOn.add(match);
		}

		public void setMatchOn(List<PatternElement> match) {
			this.matchOn = match;
		}

		public List<PatternElement> getMatchOn() {
			return matchOn;
		}

		public void setCondition(@Nullable Condition condition) {
			this.condition = Optional.ofNullable(condition).orElse(Conditions.noCondition());
		}

		public Condition getCondition() {
			return condition;
		}

		public void setReturnExpressions(Expression[] expression) {
			this.returnExpressions = Arrays.asList(expression);
		}

		public void setReturnExpression(Expression returnExpression, boolean isScalarValue) {
			this.returnExpressions = Collections.singletonList(returnExpression);
			this.scalarValueReturn = isScalarValue;
		}

		public boolean includeField(String fieldName) {
			return this.returnTuple == null || this.returnTuple.includedProperties.isEmpty() || this.returnTuple.includedProperties.contains(fieldName);
		}

		public void setOrderBy(SortItem[] orderBy) {
			this.orderBy = orderBy;
		}

		public void setLimit(Number limit) {
			this.limit = limit;
		}

		public void setSkip(Long skip) {
			this.skip = skip;
		}

		public void setReturnBasedOn(NodeDescription<?> nodeDescription, List<String> includedProperties) {
			this.returnTuple = new ReturnTuple(nodeDescription, includedProperties);
		}

		public ReturnTuple getReturnTuple() {
			return returnTuple;
		}

		public boolean isScalarValueReturn() {
			return scalarValueReturn;
		}

		public boolean isRenderConstantsAsParameters() {
			return renderConstantsAsParameters;
		}

		public void setRenderConstantsAsParameters(boolean renderConstantsAsParameters) {
			this.renderConstantsAsParameters = renderConstantsAsParameters;
		}

		private Expression[] getReturnExpressions() {
			return returnExpressions.size() > 0
					? returnExpressions.toArray(new Expression[]{})
					: CypherGenerator.INSTANCE.createReturnStatementForMatch(getReturnTuple().getNodeDescription(),
					this::includeField);
		}

		private SortItem[] getOrderBy() {
			return orderBy != null ? orderBy : new SortItem[]{};
		}

		public Statement generateGenericStatement() {
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
					.orderBy(getOrderBy())
					.returning(
							Constants.NAME_OF_ROOT_NODE.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE),
							Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
							Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES)
					)
					.skip(skip)
					.limit(limit).build();
		}

		public Statement toStatement() {

			StatementBuilder.OngoingReadingWithoutWhere match = null;

			for (PatternElement patternElement : matchOn) {
				if (match == null) {
					match = Cypher.match(matchOn.get(0));
				} else {
					match = match.match(patternElement);
				}
			}

			Statement statement = match
				.where(condition)
				.returning(getReturnExpressions())
				.orderBy(getOrderBy())
				.skip(skip)
				.limit(limit).build();
			statement.setRenderConstantsAsParameters(renderConstantsAsParameters);
			return statement;
		}

		/**
		 * Describes which fields of an entity needs to get returned.
		 */
		@API(status = API.Status.INTERNAL, since = "6.0.4")
		public final static class ReturnTuple {
			private final NodeDescription<?> nodeDescription;
			private final Set<String> includedProperties;

			private ReturnTuple(NodeDescription<?> nodeDescription, List<String> includedProperties) {
				this.nodeDescription = nodeDescription;
				this.includedProperties = includedProperties == null ? Collections.emptySet() : new HashSet<>(includedProperties);
			}

			public NodeDescription<?> getNodeDescription() {
				return nodeDescription;
			}

			public Collection<String> getIncludedProperties() {
				return includedProperties;
			}
		}
	}
}
