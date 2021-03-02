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

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.PatternElement;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

	public QueryFragmentsAndParameters(NodeDescription<?> nodeDescription, QueryFragments queryFragments, Map<String, Object> parameters) {
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

	static QueryFragmentsAndParameters forExample(Neo4jMappingContext mappingContext, Example<?> example,
												  @Nullable Pageable pageable, @Nullable Sort sort) {


		Predicate predicate = Predicate.create(mappingContext, example);
		Map<String, Object> parameters = predicate.getParameters();
		Condition condition = predicate.getCondition();

		Neo4jPersistentEntity<?> entityMetaData = mappingContext.getPersistentEntity(example.getProbeType());


		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpressions(returnStatement);

		if (pageable != null) {
			Sort pageableSort = pageable.getSort();
			long skip = pageable.getOffset();
			int pageSize = pageable.getPageSize();
			queryFragments.setSkip(skip);
			queryFragments.setLimit(pageSize);
			queryFragments.setOrderBy(CypherAdapterUtils.toSortItems(entityMetaData, pageableSort));
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

		public void addMatchOn(PatternElement match) {
			this.matchOn.add(match);
		}

		public void setMatchOn(List<PatternElement> match) {
			this.matchOn = match;
		}

		public List<PatternElement> getMatchOn() {
			return matchOn;
		}

		public void setCondition(Condition condition) {
			this.condition = condition;
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

		private Expression[] getReturnExpressions() {
			return returnExpressions.size() > 0
					? returnExpressions.toArray(new Expression[]{})
					: CypherGenerator.INSTANCE.createReturnStatementForMatch(getReturnTuple().getNodeDescription(),
					this::includeField);
		}

		private SortItem[] getOrderBy() {
			return orderBy != null ? orderBy : new SortItem[]{};
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

			return match
					.where(condition)
					.returning(getReturnExpressions())
					.orderBy(getOrderBy())
					.skip(skip)
					.limit(limit).build();
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
