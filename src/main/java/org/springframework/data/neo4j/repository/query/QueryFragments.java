/*
 * Copyright 2011-2024 the original author or authors.
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.lang.Nullable;

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
	private Collection<Expression> returnExpressions = new ArrayList<>();
	private Collection<SortItem> orderBy;
	private Number limit;
	private Long skip;
	private ReturnTuple returnTuple;
	private boolean scalarValueReturn = false;
	private Expression deleteExpression;
	/**
	 * This flag becomes {@literal true} for backward scrolling keyset pagination. Any {@code AbstractNeo4jQuery} will in turn reverse the result list.
	 */
	private boolean requiresReverseSort = false;
	private Predicate<PropertyFilter.RelaxedPropertyPath> projectingPropertyFilter;

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
		this.condition = Optional.ofNullable(condition).orElse(Cypher.noCondition());
	}

	public Condition getCondition() {
		return condition;
	}

	public void setReturnExpressions(Collection<Expression> expression) {
		this.returnExpressions = expression;
	}

	public void setDeleteExpression(Expression expression) {
		this.deleteExpression = expression;
	}

	public void setReturnExpression(Expression returnExpression, boolean isScalarValue) {
		this.returnExpressions = Collections.singletonList(returnExpression);
		this.scalarValueReturn = isScalarValue;
	}

	public void setProjectingPropertyFilter(Predicate<PropertyFilter.RelaxedPropertyPath> projectingPropertyFilter) {
		this.projectingPropertyFilter = projectingPropertyFilter;
	}

	public boolean includeField(PropertyFilter.RelaxedPropertyPath fieldName) {
		return (projectingPropertyFilter == null || projectingPropertyFilter.test(fieldName))
				&& (this.returnTuple == null || this.returnTuple.include(fieldName));
	}

	public void setOrderBy(Collection<SortItem> orderBy) {
		this.orderBy = orderBy;
	}

	public void setLimit(Number limit) {
		this.limit = limit;
	}

	public void setSkip(Long skip) {
		this.skip = skip;
	}

	public void setReturnBasedOn(NodeDescription<?> nodeDescription, Collection<PropertyFilter.ProjectedPath> includedProperties,
			boolean isDistinct, List<Expression> additionalExpressions) {
		this.returnTuple = new ReturnTuple(nodeDescription, includedProperties, isDistinct, additionalExpressions);
	}

	public boolean isScalarValueReturn() {
		return scalarValueReturn;
	}

	public void setRequiresReverseSort(boolean requiresReverseSort) {
		this.requiresReverseSort = requiresReverseSort;
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

		StatementBuilder.OngoingReadingWithWhere matchWithWhere = match.where(condition);

		if (deleteExpression != null) {
			matchWithWhere = (StatementBuilder.OngoingReadingWithWhere) matchWithWhere.detachDelete(deleteExpression);
		}

		StatementBuilder.OngoingReadingAndReturn returnPart = isDistinctReturn()
				? matchWithWhere.returningDistinct(getReturnExpressions())
				: matchWithWhere.returning(getReturnExpressions());

		Statement statement = returnPart
				.orderBy(getOrderBy())
				.skip(skip)
				.limit(limit).build();

		statement.setRenderConstantsAsParameters(false);
		return statement;
	}

	private Collection<Expression> getReturnExpressions() {
		return returnExpressions.isEmpty() ? CypherGenerator.INSTANCE.createReturnStatementForMatch((Neo4jPersistentEntity<?>) returnTuple.nodeDescription,
			this::includeField, returnTuple.additionalExpressions.toArray(Expression[]::new)) : returnExpressions;
	}

	public Collection<Expression> getAdditionalReturnExpressions() {
		return this.returnTuple == null ? List.of() : returnTuple.additionalExpressions;
	}

	private boolean isDistinctReturn() {
		return returnExpressions.isEmpty() && returnTuple.isDistinct;
	}

	public Collection<SortItem> getOrderBy() {

		if (orderBy == null) {
			return List.of();
		} else if (!requiresReverseSort) {
			return orderBy;
		} else {
			return orderBy.stream().map(QueryFragments::reverse).toList();
		}
	}

	// Yeah, would be kinda nice having a simple method in Cypher-DSL ;)
	private static SortItem reverse(SortItem sortItem) {

		var sortedExpression = new AtomicReference<Expression>();
		var sortDirection = new AtomicReference<SortItem.Direction>();

		sortItem.accept(segment -> {
			if (segment instanceof SortItem.Direction direction) {
				sortDirection.compareAndSet(null, direction == SortItem.Direction.UNDEFINED || direction == SortItem.Direction.ASC ? SortItem.Direction.DESC : SortItem.Direction.ASC);
			} else if (segment instanceof Expression expression) {
				sortedExpression.compareAndSet(null, expression);
			}
		});

		// Default might not explicitly set.
		sortDirection.compareAndSet(null, SortItem.Direction.DESC);
		return Cypher.sort(sortedExpression.get(), sortDirection.get());
	}

	public Number getLimit() {
		return limit;
	}

	public Long getSkip() {
		return skip;
	}


	/**
	 * Describes which fields of an entity needs to get returned.
	 */
	final static class ReturnTuple {
		final NodeDescription<?> nodeDescription;
		final PropertyFilter filteredProperties;
		final boolean isDistinct;
		final List<Expression> additionalExpressions;

		private ReturnTuple(NodeDescription<?> nodeDescription, Collection<PropertyFilter.ProjectedPath> filteredProperties, boolean isDistinct, List<Expression> additionalExpressions) {
			this.nodeDescription = nodeDescription;
			this.filteredProperties = PropertyFilter.from(filteredProperties, nodeDescription);
			this.isDistinct = isDistinct;
			this.additionalExpressions = List.copyOf(additionalExpressions);
		}

		boolean include(PropertyFilter.RelaxedPropertyPath fieldName) {
			String dotPath = nodeDescription.getGraphProperty(fieldName.getSegment())
					.filter(Neo4jPersistentProperty.class::isInstance)
					.map(Neo4jPersistentProperty.class::cast)
					.filter(p -> p.findAnnotation(Property.class) != null)
					.map(p -> fieldName.toDotPath(p.getPropertyName()))
					.orElseGet(fieldName::toDotPath);
			return this.filteredProperties.contains(dotPath, fieldName.getType());
		}
	}
}
