/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.cypher;

import static org.neo4j.springframework.data.core.cypher.DefaultStatementBuilder.UpdateType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingMatchAndUpdate;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReading;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingWithWhere;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingWithoutWhere;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingUpdate;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Romain Rossi
 * @since 1.0
 */
class DefaultStatementBuilder
	implements StatementBuilder,
	OngoingReading,
	OngoingUpdate,
	OngoingReadingWithWhere,
	OngoingReadingWithoutWhere, OngoingMatchAndUpdate {

	/**
	 * Current list of reading or update clauses to be generated.
	 */
	private final List<Visitable> currentSinglePartElements = new ArrayList<>();

	/**
	 * The latest ongoing match.
	 */
	private MatchBuilder currentOngoingMatch;

	/**
	 * The latest ongoing update to be build
	 */
	private DefaultStatementWithUpdateBuilder currentOngoingUpdate;

	/**
	 * A list of already build withs.
	 */
	private final List<MultiPartElement> multiPartElements = new ArrayList<>();

	@Override
	public OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern) {

		return this.match(true, pattern);
	}

	@Override
	public OngoingReadingWithoutWhere match(PatternElement... pattern) {

		return this.match(false, pattern);
	}

	private OngoingReadingWithoutWhere match(boolean optional, PatternElement... pattern) {

		Assert.notNull(pattern, "Patterns to match are required.");
		Assert.notEmpty(pattern, "At least one pattern to match is required.");

		if (this.currentOngoingMatch != null) {
			this.currentSinglePartElements.add(this.currentOngoingMatch.buildMatch());
		}
		this.currentOngoingMatch = new MatchBuilder(optional);
		this.currentOngoingMatch.patternList.addAll(Arrays.asList(pattern));
		return this;
	}

	@Override
	public OngoingUpdate create(PatternElement... pattern) {

		return update(CREATE, pattern);
	}

	@Override
	public OngoingUpdate merge(PatternElement... pattern) {

		return update(MERGE, pattern);
	}

	@Override
	public OngoingUnwind unwind(Expression expression) {
		return new DefaultOngoingUnwind(expression);
	}

	private <T extends OngoingUpdate & OngoingMatchAndUpdate> T update(UpdateType updateType, Object[] pattern) {

		Assert.notNull(pattern, "Patterns to create are required.");
		Assert.notEmpty(pattern, "At least one pattern to create is required.");

		if (this.currentOngoingMatch != null) {
			this.currentSinglePartElements.add(this.currentOngoingMatch.buildMatch());
		}

		this.currentOngoingMatch = null;

		if (this.currentOngoingUpdate != null) {
			this.currentSinglePartElements.add(this.currentOngoingUpdate.buildUpdatingClause());
		}

		if (pattern.getClass().getComponentType() == PatternElement.class) {
			this.currentOngoingUpdate = new DefaultStatementWithUpdateBuilder(updateType, (PatternElement[]) pattern);
		} else if (pattern.getClass().getComponentType() == Expression.class) {
			this.currentOngoingUpdate = new DefaultStatementWithUpdateBuilder(updateType, (Expression[]) pattern);
		}

		return (T) this;
	}

	@Override
	public OngoingReadingAndReturn returning(Expression... expressions) {

		return returning(false, expressions);
	}

	@Override
	public OngoingReadingAndReturn returningDistinct(Expression... expressions) {
		return returning(true, expressions);
	}

	private OngoingReadingAndReturn returning(boolean distinct, Expression... expressions) {

		DefaultStatementWithReturnBuilder ongoingMatchAndReturn = new DefaultStatementWithReturnBuilder(distinct);
		ongoingMatchAndReturn.addExpressions(expressions);
		return ongoingMatchAndReturn;
	}

	@Override
	public OrderableOngoingReadingAndWith with(AliasedExpression... expressions) {
		return with(false, expressions);
	}

	@Override
	public OrderableOngoingReadingAndWithWithoutWhere with(Expression... expressions) {

		return with(false, expressions);
	}

	@Override
	public OrderableOngoingReadingAndWithWithoutWhere withDistinct(Expression... expressions) {

		return with(true, expressions);
	}

	private OrderableOngoingReadingAndWithWithoutWhere with(boolean distinct, Expression... expressions) {

		DefaultStatementWithWithBuilder ongoingMatchAndWith = new DefaultStatementWithWithBuilder(distinct);
		ongoingMatchAndWith.addExpressions(expressions);
		return ongoingMatchAndWith;
	}

	@Override
	public OngoingUpdate delete(Expression... expressions) {

		return update(DELETE, expressions);
	}

	@Override
	public OngoingUpdate detachDelete(Expression... expressions) {

		return update(DETACH_DELETE, expressions);
	}

	@Override
	public OngoingMatchAndUpdate set(Expression... expressions) {
		if (this.currentOngoingUpdate != null) {
			this.currentSinglePartElements.add(this.currentOngoingUpdate.buildUpdatingClause());
			this.currentOngoingUpdate = null;
		}
		return new DefaultStatementWithUpdateBuilder(SET, expressions);
	}

	@Override
	public OngoingMatchAndUpdate set(Node named, String... label) {

		return new DefaultStatementWithUpdateBuilder(SET, Operations.set(named, label));
	}

	@Override
	public OngoingMatchAndUpdate remove(Property... properties) {

		return new DefaultStatementWithUpdateBuilder(REMOVE, properties);
	}

	@Override
	public OngoingMatchAndUpdate remove(Node named, String... label) {

		return new DefaultStatementWithUpdateBuilder(REMOVE, Operations.set(named, label));
	}

	@Override
	public OngoingReadingWithWhere where(Condition newCondition) {

		this.currentOngoingMatch.conditionBuilder.where(newCondition);
		return this;
	}

	@Override
	public OngoingReadingWithWhere and(Condition additionalCondition) {

		this.currentOngoingMatch.conditionBuilder.and(additionalCondition);
		return this;
	}

	@Override
	public OngoingReadingWithWhere or(Condition additionalCondition) {

		this.currentOngoingMatch.conditionBuilder.or(additionalCondition);
		return this;
	}

	@Override
	public Statement build() {

		return buildImpl(null);
	}

	protected Statement buildImpl(@Nullable Return returning) {
		SinglePartQuery singlePartQuery = SinglePartQuery.create(buildListOfVisitables(), returning);

		if (multiPartElements.isEmpty()) {
			return singlePartQuery;
		} else {
			return new MultiPartQuery(multiPartElements, singlePartQuery);
		}
	}

	protected final List<Visitable> buildListOfVisitables() {

		List<Visitable> visitables = new ArrayList(this.currentSinglePartElements);

		if (this.currentOngoingMatch != null) {
			visitables.add(this.currentOngoingMatch.buildMatch());
		}
		this.currentOngoingMatch = null;

		if (this.currentOngoingUpdate != null) {
			visitables.add(this.currentOngoingUpdate.buildUpdatingClause());
		}
		this.currentOngoingUpdate = null;

		this.currentSinglePartElements.clear();
		return visitables;
	}

	protected final DefaultStatementBuilder addWith(Optional<With> optionalWith) {

		optionalWith.ifPresent(with -> multiPartElements.add(new MultiPartElement(buildListOfVisitables(), with)));
		return this;
	}

	protected final DefaultStatementBuilder addUpdatingClause(UpdatingClause updatingClause) {

		// Close current match
		if (this.currentOngoingMatch != null) {
			this.currentSinglePartElements.add(this.currentOngoingMatch.buildMatch());
			this.currentOngoingMatch = null;
		}

		this.currentSinglePartElements.add(updatingClause);
		return this;
	}

	protected class DefaultStatementWithReturnBuilder
		implements OngoingReadingAndReturn, TerminalOngoingOrderDefinition, OngoingMatchAndReturnWithOrder {

		protected final List<Expression> returnList = new ArrayList<>();
		protected final OrderBuilder orderBuilder = new OrderBuilder();
		protected boolean distinct;

		protected DefaultStatementWithReturnBuilder(boolean distinct) {
			this.distinct = distinct;
		}

		@Override
		public final OngoingMatchAndReturnWithOrder orderBy(SortItem... sortItem) {
			orderBuilder.orderBy(sortItem);
			return this;
		}

		@Override
		public final TerminalOngoingOrderDefinition orderBy(Expression expression) {
			orderBuilder.orderBy(expression);
			return this;
		}

		@Override
		public final TerminalOngoingOrderDefinition and(Expression expression) {
			orderBuilder.and(expression);
			return this;
		}

		@Override
		public final OngoingReadingAndReturn descending() {
			orderBuilder.descending();
			return this;
		}

		@Override
		public final OngoingReadingAndReturn ascending() {
			orderBuilder.ascending();
			return this;
		}

		@Override
		public final OngoingReadingAndReturn skip(@Nullable Number number) {
			orderBuilder.skip(number);
			return this;
		}

		@Override
		public final OngoingReadingAndReturn limit(@Nullable Number number) {
			orderBuilder.limit(number);
			return this;
		}

		@Override
		public Statement build() {

			Return returning = null;
			if (!returnList.isEmpty()) {

				ExpressionList returnItems = new ExpressionList(this.returnList);
				returning = new Return(distinct, returnItems, orderBuilder.buildOrder().orElse(null), orderBuilder.getSkip(),
					orderBuilder.getLimit());
			}

			return DefaultStatementBuilder.this.buildImpl(returning);
		}

		protected final void addExpressions(Expression... expressions) {

			Assert.notNull(expressions, "Expressions to return are required.");
			Assert.notEmpty(expressions, "At least one expressions to return is required.");

			this.returnList.addAll(Arrays.asList(expressions));
		}
	}

	/**
	 * Adds support for With to a return builder.
	 */
	protected abstract class WithBuilderSupport {


	}

	/**
	 * Ongoing with extends from {@link WithBuilderSupport} and therefore from {@link DefaultStatementWithReturnBuilder}.
	 */
	protected final class DefaultStatementWithWithBuilder extends WithBuilderSupport
		implements OngoingReadingAndWith, OngoingOrderDefinition, OrderableOngoingReadingAndWithWithoutWhere,
		OrderableOngoingReadingAndWithWithWhere, OngoingReadingAndWithWithWhereAndOrder {

		protected final ConditionBuilder conditionBuilder = new ConditionBuilder();
		protected final List<Expression> returnList = new ArrayList<>();
		protected final OrderBuilder orderBuilder = new OrderBuilder();
		protected boolean distinct;

		protected DefaultStatementWithWithBuilder(boolean distinct) {
			this.distinct = distinct;
		}

		protected Optional<With> buildWith() {

			if (returnList.isEmpty()) {
				return Optional.empty();
			}

			ExpressionList returnItems = new ExpressionList(returnList);

			Where where = conditionBuilder.buildCondition().map(Where::new).orElse(null);

			Optional<With> returnedWith = Optional
				.of(new With(distinct, returnItems, orderBuilder.buildOrder().orElse(null), orderBuilder.getSkip(),
					orderBuilder.getLimit(), where));
			this.returnList.clear();
			this.orderBuilder.reset();
			return returnedWith;
		}

		protected void addExpressions(Expression... expressions) {

			Assert.notNull(expressions, "Expressions to return are required.");
			Assert.notEmpty(expressions, "At least one expressions to return is required.");

			this.returnList.addAll(Arrays.asList(expressions));
		}

		@Override
		public OngoingReadingAndReturn returning(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.returning(expressions);
		}

		@Override
		public OngoingReadingAndReturn returningDistinct(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.returningDistinct(expressions);
		}

		@Override
		public OngoingUpdate delete(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.delete(expressions);
		}

		@Override
		public OngoingUpdate detachDelete(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.detachDelete(expressions);
		}

		@Override
		public OngoingMatchAndUpdate set(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.set(expressions);
		}

		@Override
		public OngoingMatchAndUpdate set(Node node, String... label) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.set(node, label);
		}

		@Override
		public OngoingMatchAndUpdate remove(Node node, String... label) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.remove(node, label);
		}

		@Override
		public OngoingMatchAndUpdate remove(Property... properties) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.remove(properties);
		}

		@Override
		public OrderableOngoingReadingAndWithWithoutWhere with(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.with(expressions);
		}

		@Override
		public OrderableOngoingReadingAndWithWithoutWhere withDistinct(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.withDistinct(expressions);
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere where(Condition newCondition) {

			conditionBuilder.where(newCondition);
			return this;
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere and(Condition additionalCondition) {

			conditionBuilder.and(additionalCondition);
			return this;
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere or(Condition additionalCondition) {

			conditionBuilder.or(additionalCondition);
			return this;
		}

		@Override
		public OngoingReadingWithoutWhere match(PatternElement... pattern) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.match(pattern);
		}

		@Override
		public OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.optionalMatch(pattern);
		}

		@Override
		public OngoingUpdate create(PatternElement... pattern) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.create(pattern);
		}

		@Override
		public OngoingUpdate merge(PatternElement... pattern) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.merge(pattern);
		}

		@Override
		public OngoingUnwind unwind(Expression expression) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.unwind(expression);
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere orderBy(SortItem... sortItem) {
			orderBuilder.orderBy(sortItem);
			return this;
		}

		@Override
		public OngoingOrderDefinition orderBy(Expression expression) {
			orderBuilder.orderBy(expression);
			return this;
		}

		@Override
		public OngoingOrderDefinition and(Expression expression) {
			orderBuilder.and(expression);
			return this;
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere descending() {
			orderBuilder.descending();
			return this;
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere ascending() {
			orderBuilder.ascending();
			return this;
		}

		@Override
		public OrderableOngoingReadingAndWithWithWhere skip(@Nullable Number number) {
			orderBuilder.skip(number);
			return this;
		}

		@Override
		public OngoingReadingAndWith limit(@Nullable Number number) {
			orderBuilder.limit(number);
			return this;
		}
	}

	/**
	 * A private enum for distinguishing updating clauses.
	 */
	enum UpdateType {
		DELETE, DETACH_DELETE, SET, REMOVE,
		CREATE, MERGE;
	}

	private static final EnumSet<UpdateType> MERGE_OR_CREATE = EnumSet.of(CREATE, MERGE);

	protected final class DefaultStatementWithUpdateBuilder extends DefaultStatementWithReturnBuilder
		implements OngoingMatchAndUpdate, OngoingReadingAndReturn {

		private final List<? extends Visitable> expressions;
		private final UpdateType updateType;

		protected DefaultStatementWithUpdateBuilder(UpdateType updateType, PatternElement... pattern) {
			super(false);

			this.updateType = updateType;

			Assert.notNull(pattern, "Patterns to create are required.");
			Assert.notEmpty(pattern, "At least one pattern to create is required.");

			this.expressions = Arrays.asList(pattern);
		}

		protected DefaultStatementWithUpdateBuilder(UpdateType updateType, Expression... expressions) {
			super(false);

			this.updateType = updateType;

			Assert.notNull(expressions, "Modifying expressions are required.");
			Assert.notEmpty(expressions, "At least one expressions is required.");

			switch (this.updateType) {
				case DETACH_DELETE:
				case DELETE:
					this.expressions = Arrays.asList(expressions);
					break;
				case SET:
					this.expressions = prepareSetExpressions(expressions);
					break;
				case REMOVE:
					this.expressions = Arrays.asList(expressions);
					break;
				default:
					throw new IllegalArgumentException("Unsupported update type " + updateType);
			}
		}

		List<? extends Visitable> prepareSetExpressions(Expression... possibleSetOperations) {
			List<Expression> propertyOperations = new ArrayList<>();

			List<Expression> listOfExpressions = new ArrayList<>();
			for (Expression possibleSetOperation : possibleSetOperations) {
				if (possibleSetOperation instanceof Operation) {
					propertyOperations.add(possibleSetOperation);
				} else {
					listOfExpressions.add(possibleSetOperation);
				}

			}

			if (listOfExpressions.size() % 2 != 0) {
				throw new IllegalArgumentException("The list of expression to set must be even.");
			}
			for (int i = 0; i < listOfExpressions.size(); i += 2) {
				propertyOperations.add(Operations.set(listOfExpressions.get(i), listOfExpressions.get(i + 1)));
			}

			return propertyOperations;
		}

		@Override
		public OngoingReadingAndReturn returning(Expression... returnedExpressions) {

			Assert.notNull(returnedExpressions, "Expressions to return are required.");
			Assert.notEmpty(returnedExpressions, "At least one expressions to return is required.");

			super.returnList.addAll(Arrays.asList(returnedExpressions));
			return this;
		}

		@Override
		public OngoingReadingAndReturn returningDistinct(Expression... returnedExpressions) {

			returning(returnedExpressions);
			super.distinct = true;
			return this;
		}

		@Override
		public OngoingUpdate delete(Expression... deletedExpressions) {
			return delete(false, deletedExpressions);
		}

		@Override
		public OngoingUpdate detachDelete(Expression... deletedExpressions) {
			return delete(true, deletedExpressions);
		}

		@Override
		public <T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern) {
			throw new UnsupportedOperationException("Not supported yet");
		}

		private OngoingUpdate delete(boolean nextDetach, Expression... deletedExpressions) {
			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this.update(nextDetach ? DETACH_DELETE : DELETE, deletedExpressions);
		}

		@Override
		public OngoingMatchAndUpdate set(Expression... keyValuePairs) {

			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this.new DefaultStatementWithUpdateBuilder(SET, keyValuePairs);
		}

		@Override
		public OngoingMatchAndUpdate set(Node node, String... label) {

			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this.new DefaultStatementWithUpdateBuilder(SET, Operations.set(node, label));
		}

		@Override
		public OngoingMatchAndUpdate remove(Node node, String... label) {

			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this.new DefaultStatementWithUpdateBuilder(REMOVE,
				Operations.set(node, label));
		}

		@Override
		public OngoingMatchAndUpdate remove(Property... properties) {

			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this.new DefaultStatementWithUpdateBuilder(REMOVE, properties);
		}

		@Override
		public OrderableOngoingReadingAndWithWithoutWhere with(Expression... returnedExpressions) {
			return this.with(false, returnedExpressions);
		}

		@Override
		public OrderableOngoingReadingAndWithWithoutWhere withDistinct(Expression... returnedExpressions) {
			return this.with(true, returnedExpressions);
		}

		private OrderableOngoingReadingAndWithWithoutWhere with(boolean distinct, Expression... returnedExpressions) {
			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this
				.with(distinct, returnedExpressions);
		}

		@Override
		public Statement build() {

			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return super.build();
		}

		private UpdatingClause buildUpdatingClause() {

			if (MERGE_OR_CREATE.contains(updateType)) {
				final Pattern pattern = new Pattern(this.expressions);
				switch (updateType) {
					case CREATE:
						return new Create(pattern);
					case MERGE:
						return new Merge(pattern);
				}
			} else {
				final ExpressionList expressionsList = new ExpressionList(this.expressions);
				switch (updateType) {
					case DETACH_DELETE:
						return new Delete(expressionsList, true);
					case DELETE:
						return new Delete(expressionsList, false);
					case SET:
						return new Set(expressionsList);
					case REMOVE:
						return new Remove(expressionsList);
				}
			}

			throw new IllegalArgumentException("Unsupported update type " + updateType);
		}
	}

	// Static builder and support classes

	static final class MatchBuilder {

		private final List<PatternElement> patternList = new ArrayList<>();

		private final ConditionBuilder conditionBuilder = new ConditionBuilder();

		private final boolean optional;

		MatchBuilder(boolean optional) {
			this.optional = optional;
		}

		Match buildMatch() {
			Pattern pattern = new Pattern(this.patternList);
			return new Match(optional, pattern, conditionBuilder.buildCondition().map(Where::new).orElse(null));
		}
	}

	final class DefaultOngoingUnwind implements OngoingUnwind {

		private final Expression expressionToUnwind;

		DefaultOngoingUnwind(Expression expressionToUnwind) {
			this.expressionToUnwind = expressionToUnwind;
		}

		@Override
		public OngoingReading as(String variable) {
			DefaultStatementBuilder.this.currentSinglePartElements.add(new Unwind(expressionToUnwind, variable));
			return DefaultStatementBuilder.this;
		}
	}

	static final class ConditionBuilder {
		protected Condition condition;

		void where(Condition newCondition) {

			Assert.notNull(newCondition, "The new condition must not be null.");
			this.condition = newCondition;
		}

		void and(Condition additionalCondition) {

			this.condition = this.condition.and(additionalCondition);
		}

		void or(Condition additionalCondition) {

			this.condition = this.condition.or(additionalCondition);
		}

		private boolean hasCondition() {
			return !(this.condition == null || this.condition == CompoundCondition.EMPTY_CONDITION);
		}

		Optional<Condition> buildCondition() {
			return hasCondition() ? Optional.of(this.condition) : Optional.empty();
		}
	}

	static final class OrderBuilder {
		protected final List<SortItem> sortItemList = new ArrayList<>();
		protected SortItem lastSortItem;
		protected Skip skip;
		protected Limit limit;

		protected void reset() {
			this.sortItemList.clear();
			this.lastSortItem = null;
			this.skip = null;
			this.limit = null;
		}

		protected void orderBy(SortItem... sortItem) {
			Arrays.stream(sortItem).forEach(this.sortItemList::add);
		}

		protected void orderBy(Expression expression) {
			this.lastSortItem = Cypher.sort(expression);
		}

		protected void and(Expression expression) {
			orderBy(expression);
		}

		protected void descending() {
			this.sortItemList.add(this.lastSortItem.descending());
			this.lastSortItem = null;
		}

		protected void ascending() {
			this.sortItemList.add(this.lastSortItem.ascending());
			this.lastSortItem = null;
		}

		protected void skip(@Nullable Number number) {

			if (number != null) {
				skip = Skip.create(number);
			}
		}

		protected void limit(@Nullable Number number) {

			if (number != null) {
				limit = Limit.create(number);
			}
		}

		protected Optional<Order> buildOrder() {
			if (lastSortItem != null) {
				sortItemList.add(lastSortItem);
			}
			return sortItemList.size() > 0 ? Optional.of(new Order(sortItemList)) : Optional.empty();
		}

		protected Skip getSkip() {
			return skip;
		}

		protected Limit getLimit() {
			return limit;
		}
	}
}
