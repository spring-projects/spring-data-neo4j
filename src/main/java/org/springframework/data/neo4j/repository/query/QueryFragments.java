package org.springframework.data.neo4j.repository.query;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.neo4j.core.mapping.NodeDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QueryFragments {
	private List<PatternElement> matchOn = new ArrayList<>();
	private Condition condition;
	private List<Expression> returnExpressions = new ArrayList<>();
	private SortItem[] orderBy;
	private Number limit;
	private Long skip;
	private ReturnTuple returnTuple;

	public void setMatchOn(PatternElement match) {
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

	public void setReturnExpression(Expression[] returnExpressions) {
		this.returnExpressions = Arrays.asList(returnExpressions);
	}

	public void addReturnExpression(Expression returnExpression) {
		this.returnExpressions.add(returnExpression);
	}


	public void setOrderBy(SortItem[] orderBy) {
		this.orderBy = orderBy;
	}

	public SortItem[] getOrderBy() {
		return orderBy != null ? orderBy : new SortItem[]{};
	}

	public void setLimit(Number limit) {
		this.limit = limit;
	}

	public Number getLimit() {
		return limit;
	}

	public void setSkip(Long skip) {
		this.skip = skip;
	}

	public Long getSkip() {
		return skip;
	}

	public void setReturnBasedOn(NodeDescription<?> nodeDescription, List<String> includedProperties) {
		this.returnTuple = new ReturnTuple(nodeDescription, includedProperties);
	}

	public Expression[] getReturn() {
		return returnExpressions.toArray(new Expression[]{});
	}

	public ReturnTuple getReturnTuple() {
		return returnTuple;
	}

	public static class ReturnTuple {
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
