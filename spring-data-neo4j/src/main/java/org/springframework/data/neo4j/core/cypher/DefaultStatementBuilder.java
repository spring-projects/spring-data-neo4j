/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.core.cypher;

import static org.springframework.data.neo4j.core.cypher.DefaultStatementBuilder.UpdateType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingReading;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingReadingWithWhere;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingReadingWithoutWhere;
import org.springframework.data.neo4j.core.cypher.support.Visitable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
class DefaultStatementBuilder
	implements StatementBuilder,
	OngoingReading,
	StatementBuilder.OngoingUpdate,
	OngoingReadingWithWhere,
	OngoingReadingWithoutWhere {

	/**
	 * Current list of reading or update clauses to be generated.
	 */
	private final List<Visitable> currentSinglePartElements = new ArrayList<>();

	/**
	 * The latest ongoing match.
	 */
	private DefaultMatchBuilder currentOngoingMatch;

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
		this.currentOngoingMatch = new DefaultMatchBuilder(optional);
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
		return new DefaultUnwindBuilder(expression);
	}

	private OngoingUpdate update(UpdateType updateType, PatternElement... pattern) {

		Assert.notNull(pattern, "Patterns to create are required.");
		Assert.notEmpty(pattern, "At least one pattern to create is required.");
		Assert.isTrue(MERGE_OR_CREATE.contains(updateType),
			"Only CREATE and MERGE clauses can be used without a preceding reading clause.");

		if (this.currentOngoingMatch != null) {
			this.currentSinglePartElements.add(this.currentOngoingMatch.buildMatch());
		}
		this.currentOngoingMatch = null;

		if (this.currentOngoingUpdate != null) {
			this.currentSinglePartElements.add(this.currentOngoingUpdate.buildUpdatingClause());
		}

		this.currentOngoingUpdate = new DefaultStatementWithUpdateBuilder(updateType, pattern);
		return this;
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
	public OngoingReadingAndWith with(AliasedExpression... expressions) {
		return with(false, expressions);
	}

	@Override
	public OngoingReadingAndWithWithoutWhere with(Expression... expressions) {

		return with(false, expressions);
	}

	@Override
	public OngoingReadingAndWithWithoutWhere withDistinct(Expression... expressions) {

		return with(true, expressions);
	}

	private OngoingReadingAndWithWithoutWhere with(boolean distinct, Expression... expressions) {

		DefaultStatementWithWithBuilder ongoingMatchAndWith = new DefaultStatementWithWithBuilder(distinct);
		ongoingMatchAndWith.addExpressions(expressions);
		return ongoingMatchAndWith;
	}

	@Override
	public OngoingMatchAndUpdate delete(Expression... expressions) {

		return new DefaultStatementWithUpdateBuilder(DELETE, expressions);
	}

	@Override
	public OngoingMatchAndUpdate detachDelete(Expression... expressions) {

		return new DefaultStatementWithUpdateBuilder(DETACH_DELETE, expressions);
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
		implements OngoingReadingAndReturn, OngoingOrderDefinition, OngoingMatchAndReturnWithOrder {

		protected final List<Expression> returnList = new ArrayList<>();
		protected final List<SortItem> sortItemList = new ArrayList<>();
		protected boolean distinct;
		protected SortItem lastSortItem;
		protected Skip skip;
		protected Limit limit;

		protected DefaultStatementWithReturnBuilder(boolean distinct) {
			this.distinct = distinct;
		}

		@Override
		public final OngoingReadingAndReturn orderBy(SortItem... sortItem) {
			Arrays.stream(sortItem).forEach(this.sortItemList::add);
			return this;
		}

		@Override
		public final OngoingOrderDefinition orderBy(Expression expression) {
			this.lastSortItem = Cypher.sort(expression);
			return this;
		}

		@Override
		public final OngoingOrderDefinition and(Expression expression) {
			return orderBy(expression);
		}

		@Override
		public final OngoingReadingAndReturn descending() {
			this.sortItemList.add(this.lastSortItem.descending());
			this.lastSortItem = null;
			return this;
		}

		@Override
		public final OngoingReadingAndReturn ascending() {
			this.sortItemList.add(this.lastSortItem.ascending());
			this.lastSortItem = null;
			return this;
		}

		@Override
		public final OngoingReadingAndReturn skip(@Nullable Number number) {

			if (number != null) {
				skip = Skip.create(number);
			}
			return this;
		}

		@Override
		public final OngoingReadingAndReturn limit(@Nullable Number number) {

			if (number != null) {
				limit = Limit.create(number);
			}
			return this;
		}

		@Override
		public Statement build() {

			Return returning = null;
			if (!returnList.isEmpty()) {

				ExpressionList returnItems = new ExpressionList(this.returnList);

				if (lastSortItem != null) {
					sortItemList.add(lastSortItem);
				}
				Order order = sortItemList.size() > 0 ? new Order(sortItemList) : null;
				returning = new Return(distinct, returnItems, order, skip, limit);
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
	protected abstract class WithBuilderSupport extends DefaultStatementWithReturnBuilder {
		protected final DefaultConditionBuilder conditionBuilder = new DefaultConditionBuilder();

		protected WithBuilderSupport(boolean distinct) {
			super(distinct);
		}

		protected final Optional<With> buildWith() {

			if (returnList.isEmpty()) {
				return Optional.empty();
			}

			ExpressionList returnItems = new ExpressionList(returnList);

			if (lastSortItem != null) {
				sortItemList.add(lastSortItem);
			}
			Order order = sortItemList.size() > 0 ? new Order(sortItemList) : null;
			Where where = conditionBuilder.buildCondition().map(Where::new).orElse(null);

			return Optional.of(new With(distinct, returnItems, order, skip, limit, where));
		}
	}

	/**
	 * Ongoing with extends from {@link WithBuilderSupport} and therefore from {@Defaultd}
	 */
	protected final class DefaultStatementWithWithBuilder extends WithBuilderSupport
		implements OngoingReadingAndWithWithoutWhere, OngoingReadingAndWithWithWhere {

		protected DefaultStatementWithWithBuilder(boolean distinct) {
			super(distinct);
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
				.returning(expressions);
		}

		@Override
		public OngoingMatchAndUpdate delete(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.delete(expressions);
		}

		@Override
		public OngoingMatchAndUpdate detachDelete(Expression... expressions) {
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
		public OngoingReadingAndWithWithoutWhere with(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.with(expressions);
		}

		@Override
		public OngoingReadingAndWithWithoutWhere withDistinct(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.withDistinct(expressions);
		}

		@Override
		public OngoingReadingAndWithWithWhere where(Condition newCondition) {

			super.conditionBuilder.where(newCondition);
			return this;
		}

		@Override
		public OngoingReadingAndWithWithWhere and(Condition additionalCondition) {

			super.conditionBuilder.and(additionalCondition);
			return this;
		}

		@Override
		public OngoingReadingAndWithWithWhere or(Condition additionalCondition) {

			super.conditionBuilder.or(additionalCondition);
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
	}

	/**
	 * A private enum for distinguishing updating clauses.
	 */
	enum UpdateType {
		DELETE, DETACH_DELETE, SET, REMOVE,
		CREATE, MERGE;
	}

	private static final EnumSet<UpdateType> MERGE_OR_CREATE = EnumSet.of(CREATE, MERGE);

	protected final class DefaultStatementWithUpdateBuilder extends WithBuilderSupport
		implements OngoingMatchAndUpdate {

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
		public OngoingMatchAndUpdate delete(Expression... deletedExpressions) {
			return delete(false, deletedExpressions);
		}

		@Override
		public OngoingMatchAndUpdate detachDelete(Expression... deletedExpressions) {
			return delete(true, deletedExpressions);
		}

		@Override
		public <T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern) {
			throw new UnsupportedOperationException("Not supported yet");
		}

		private OngoingMatchAndUpdate delete(boolean nextDetach, Expression... deletedExpressions) {
			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this.new DefaultStatementWithUpdateBuilder(
				nextDetach ? DETACH_DELETE : DELETE, deletedExpressions);
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
		public OngoingReadingAndWithWithoutWhere with(Expression... returnedExpressions) {
			return this.with(false, returnedExpressions);
		}

		@Override
		public OngoingReadingAndWithWithoutWhere withDistinct(Expression... returnedExpressions) {
			return this.with(true, returnedExpressions);
		}

		private OngoingReadingAndWithWithoutWhere with(boolean distinct, Expression... returnedExpressions) {
			DefaultStatementBuilder.this.addUpdatingClause(buildUpdatingClause());
			return DefaultStatementBuilder.this
				.addWith(buildWith())
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

	static final class DefaultMatchBuilder {

		private final List<PatternElement> patternList = new ArrayList<>();

		private final DefaultConditionBuilder conditionBuilder = new DefaultConditionBuilder();

		private final boolean optional;

		DefaultMatchBuilder(boolean optional) {
			this.optional = optional;
		}

		Match buildMatch() {
			Pattern pattern = new Pattern(this.patternList);
			return new Match(optional, pattern, conditionBuilder.buildCondition().map(Where::new).orElse(null));
		}
	}

	final class DefaultUnwindBuilder implements OngoingUnwind {

		private final Expression expressionToUnwind;

		DefaultUnwindBuilder(Expression expressionToUnwind) {
			this.expressionToUnwind = expressionToUnwind;
		}

		@Override
		public OngoingReading as(String variable) {
			DefaultStatementBuilder.this.currentSinglePartElements.add(new Unwind(expressionToUnwind, variable));
			return DefaultStatementBuilder.this;
		}
	}

	static final class DefaultConditionBuilder {
		protected Condition condition;

		void where(Condition newCondition) {

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
}
