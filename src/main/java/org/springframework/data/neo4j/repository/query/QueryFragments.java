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
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.NodeDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Collects the parts of a Cypher query to be handed over to the Cypher generator.
 *
 * @author Gerrit Meier
 * @since 6.0.4
 */
@API(status = API.Status.INTERNAL, since = "6.0.4")
public final class QueryFragments {
	private List<PatternElement> matchOn = new ArrayList<>();
	private Condition condition;
	private List<Expression> returnExpressions = new ArrayList<>();
	private SortItem[] orderBy;
	private Number limit;
	private Long skip;
	private ReturnTuple returnTuple;

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

	public void setReturnExpression(Expression[] expression) {
		this.returnExpressions = Arrays.asList(expression);
	}

	public void addReturnExpression(Expression returnExpression) {
		this.returnExpressions.add(returnExpression);
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

	private Expression[] getReturnExpressions() {
		return returnExpressions.size() > 0
				? returnExpressions.toArray(new Expression[]{})
				: CypherGenerator.INSTANCE.createReturnStatementForMatch(getReturnTuple().getNodeDescription(),
				getReturnTuple().getIncludedProperties());
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
		private final List<String> includedProperties;

		private ReturnTuple(NodeDescription<?> nodeDescription, List<String> includedProperties) {
			this.nodeDescription = nodeDescription;
			this.includedProperties = includedProperties;
		}

		public NodeDescription<?> getNodeDescription() {
			return nodeDescription;
		}

		public List<String> getIncludedProperties() {
			return includedProperties;
		}
	}
}
