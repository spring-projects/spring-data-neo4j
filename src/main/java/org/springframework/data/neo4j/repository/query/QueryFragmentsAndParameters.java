package org.springframework.data.neo4j.repository.query;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Expression;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.lang.Nullable;

import java.util.Map;

public class QueryFragmentsAndParameters {
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

		CypherGenerator cypherGenerator = CypherGenerator.INSTANCE;

		Predicate predicate = Predicate.create(mappingContext, example);
		Map<String, Object> parameters = predicate.getParameters();
		Condition condition = predicate.getCondition();

		Neo4jPersistentEntity<?> entityMetaData = mappingContext.getPersistentEntity(example.getProbeType());


		Expression[] returnStatement = cypherGenerator.createReturnStatementForMatch(entityMetaData);

		QueryFragments queryFragments = new QueryFragments();
		queryFragments.setMatchOn(cypherGenerator.createRootNode(entityMetaData));
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
