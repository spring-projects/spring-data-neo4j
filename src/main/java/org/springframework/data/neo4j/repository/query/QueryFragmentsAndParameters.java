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
import org.neo4j.cypherdsl.core.Expression;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Map;

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

	public static QueryFragmentsAndParameters findById(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_ID, idValues);

		Condition condition = entityMetaData.getIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID));
		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpression(returnStatement);
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
	}

	public static QueryFragmentsAndParameters findByAllId(Neo4jPersistentEntity<?> entityMetaData, Object idValues) {
		Map<String, Object> parameters = Collections.singletonMap(Constants.NAME_OF_IDS, idValues);

		Condition condition = entityMetaData.getIdExpression().in((parameter(Constants.NAME_OF_IDS)));
		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);
		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpression(returnStatement);
		return new QueryFragmentsAndParameters(entityMetaData, queryFragments, parameters);
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
	 * Following methods are used by the Simple(Reactive)QueryByExampleExecutor
	 */
	static QueryFragmentsAndParameters of(Neo4jMappingContext mappingContext, Example<?> example) {
		return QueryFragmentsAndParameters.of(mappingContext, example, null, null);
	}

	static QueryFragmentsAndParameters of(Neo4jMappingContext mappingContext, Example<?> example, Sort sort) {
		return QueryFragmentsAndParameters.of(mappingContext, example, null, sort);
	}

	static QueryFragmentsAndParameters of(Neo4jMappingContext mappingContext, Example<?> example, Pageable pageable) {
		return QueryFragmentsAndParameters.of(mappingContext, example, pageable, null);
	}

	static QueryFragmentsAndParameters of(Neo4jMappingContext mappingContext, Example<?> example,
										  @Nullable Pageable pageable, @Nullable Sort sort) {


		Predicate predicate = Predicate.create(mappingContext, example);
		Map<String, Object> parameters = predicate.getParameters();
		Condition condition = predicate.getCondition();

		Neo4jPersistentEntity<?> entityMetaData = mappingContext.getPersistentEntity(example.getProbeType());


		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.addMatchOn(cypherGenerator.createRootNode(entityMetaData));
		queryFragments.setCondition(condition);
		queryFragments.setReturnExpression(returnStatement);

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
}
