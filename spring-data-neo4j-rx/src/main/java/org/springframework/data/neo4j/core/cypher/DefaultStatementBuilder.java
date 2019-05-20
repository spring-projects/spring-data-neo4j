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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatch;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchWithWhere;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchWithoutWhere;
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
	OngoingMatch,
	OngoingMatchWithWhere,
	OngoingMatchWithoutWhere {

	/**
	 * Current list of reading or update clauses to be generated.
	 */
	private final List<Visitable> currentSinglePartElements = new ArrayList<>();

	/**
	 * The latest ongoing match.
	 */
	private DefaultMatchBuilder currentOngoingMatch;

	/**
	 * A list of already build withs.
	 */
	private final List<MultiPartElement> multiPartElements = new ArrayList<>();

	@Override
	public OngoingMatchWithoutWhere optionalMatch(PatternElement... pattern) {

		return this.match(true, pattern);
	}

	@Override
	public OngoingMatchWithoutWhere match(PatternElement... pattern) {

		return this.match(false, pattern);
	}

	private OngoingMatchWithoutWhere match(boolean optional, PatternElement... pattern) {

		Assert.notNull(pattern, "Patterns to match are required.");
		Assert.notEmpty(pattern, "At least one pattern to match is required.");
		if (this.currentOngoingMatch != null) {
			this.currentSinglePartElements.add(this.currentOngoingMatch.buildMatch());
		}
		this.currentOngoingMatch = new DefaultMatchBuilder(optional);
		this.currentOngoingMatch.matchList.addAll(Arrays.asList(pattern));
		return this;
	}

	@Override
	public OngoingMatchAndReturn returning(Expression... expressions) {

		return returning(false, expressions);
	}

	@Override
	public OngoingMatchAndReturn returningDistinct(Expression... expressions) {
		return returning(true, expressions);
	}

	private OngoingMatchAndReturn returning(boolean distinct, Expression... expressions) {

		DefaultStatementWithReturnBuilder ongoingMatchAndReturn = new DefaultStatementWithReturnBuilder(distinct);
		ongoingMatchAndReturn.addExpressions(expressions);
		return ongoingMatchAndReturn;
	}

	@Override
	public OngoingMatchAndWithWithoutWhere with(Expression... expressions) {

		return with(false, expressions);
	}

	@Override
	public OngoingMatchAndWithWithoutWhere withDistinct(Expression... expressions) {

		return with(true, expressions);
	}

	private OngoingMatchAndWithWithoutWhere with(boolean distinct, Expression... expressions) {

		DefaultStatementWithWithBuilder ongoingMatchAndWith = new DefaultStatementWithWithBuilder(distinct);
		ongoingMatchAndWith.addExpressions(expressions);
		return ongoingMatchAndWith;
	}

	@Override
	public OngoingMatchAndDelete delete(Expression... expressions) {

		return new DefaultStatementWithDeleteBuilder(false, expressions);
	}

	@Override
	public OngoingMatchAndDelete detachDelete(Expression... expressions) {

		return new DefaultStatementWithDeleteBuilder(true, expressions);
	}

	@Override
	public OngoingMatchWithWhere where(Condition newCondition) {

		this.currentOngoingMatch.conditionBuilder.where(newCondition);
		return this;
	}

	@Override
	public OngoingMatchWithWhere and(Condition additionalCondition) {

		this.currentOngoingMatch.conditionBuilder.and(additionalCondition);
		return this;
	}

	@Override
	public OngoingMatchWithWhere or(Condition additionalCondition) {

		this.currentOngoingMatch.conditionBuilder.or(additionalCondition);
		return this;
	}

	protected final List<Visitable> buildMatchList() {
		List<Visitable> completeMatchesList = new ArrayList(this.currentSinglePartElements);
		if (this.currentOngoingMatch != null) {
			completeMatchesList.add(this.currentOngoingMatch.buildMatch());
		}
		this.currentOngoingMatch = null;
		this.currentSinglePartElements.clear();
		return completeMatchesList;
	}

	protected final DefaultStatementBuilder addWith(Optional<With> optionalWith) {

		optionalWith.ifPresent(with -> {
			multiPartElements.add(new MultiPartElement(buildMatchList(), with));
		});
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
		implements OngoingMatchAndReturn, OngoingOrderDefinition, OngoingMatchAndReturnWithOrder {

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
		public final OngoingMatchAndReturn orderBy(SortItem... sortItem) {
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
		public final OngoingMatchAndReturn descending() {
			this.sortItemList.add(this.lastSortItem.descending());
			this.lastSortItem = null;
			return this;
		}

		@Override
		public final OngoingMatchAndReturn ascending() {
			this.sortItemList.add(this.lastSortItem.ascending());
			this.lastSortItem = null;
			return this;
		}

		@Override
		public final OngoingMatchAndReturn skip(@Nullable Number number) {

			if (number != null) {
				skip = Skip.create(number);
			}
			return this;
		}

		@Override
		public final OngoingMatchAndReturn limit(@Nullable Number number) {

			if (number != null) {
				limit = Limit.create(number);
			}
			return this;
		}

		@Override
		public Statement build() {
			SinglePartQuery singlePartQuery = SinglePartQuery.create(buildMatchList(), buildReturn().orElse(null));

			if (multiPartElements.isEmpty()) {
				return singlePartQuery;
			} else {
				return new MultiPartQuery(multiPartElements, singlePartQuery);
			}
		}

		protected final void addExpressions(Expression... expressions) {

			Assert.notNull(expressions, "Expressions to return are required.");
			Assert.notEmpty(expressions, "At least one expressions to return is required.");

			this.returnList.addAll(Arrays.asList(expressions));
		}

		protected final Optional<Return> buildReturn() {

			if (returnList.isEmpty()) {
				return Optional.empty();
			}

			ExpressionList returnItems = new ExpressionList(this.returnList);

			if (lastSortItem != null) {
				sortItemList.add(lastSortItem);
			}
			Order order = sortItemList.size() > 0 ? new Order(sortItemList) : null;
			return Optional.of(new Return(distinct, returnItems, order, skip, limit));
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
		implements OngoingMatchAndWithWithoutWhere, OngoingMatchAndWithWithWhere {

		protected DefaultStatementWithWithBuilder(boolean distinct) {
			super(distinct);
		}

		@Override
		public OngoingMatchAndReturn returning(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.returning(expressions);
		}

		@Override
		public OngoingMatchAndReturn returningDistinct(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.returning(expressions);
		}

		@Override
		public OngoingMatchAndDelete delete(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.delete(expressions);
		}

		@Override
		public OngoingMatchAndDelete detachDelete(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.detachDelete(expressions);
		}

		@Override
		public OngoingMatchAndWithWithoutWhere with(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.with(expressions);
		}

		@Override
		public OngoingMatchAndWithWithoutWhere withDistinct(Expression... expressions) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.withDistinct(expressions);
		}

		@Override
		public OngoingMatchAndWithWithWhere where(Condition newCondition) {

			super.conditionBuilder.where(newCondition);
			return this;
		}

		@Override
		public OngoingMatchAndWithWithWhere and(Condition additionalCondition) {

			super.conditionBuilder.and(additionalCondition);
			return this;
		}

		@Override
		public OngoingMatchAndWithWithWhere or(Condition additionalCondition) {

			super.conditionBuilder.or(additionalCondition);
			return this;
		}

		@Override
		public OngoingMatchWithoutWhere match(PatternElement... pattern) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.match(pattern);
		}

		@Override
		public OngoingMatchWithoutWhere optionalMatch(PatternElement... pattern) {

			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.optionalMatch(pattern);
		}
	}

	protected final class DefaultStatementWithDeleteBuilder extends WithBuilderSupport
		implements OngoingMatchAndDelete {

		private final List<Expression> deleteList;
		private final boolean detach;

		protected DefaultStatementWithDeleteBuilder(boolean detach, Expression... expressions) {
			super(false);
			this.detach = detach;

			Assert.notNull(expressions, "Expressions to delete are required.");
			Assert.notEmpty(expressions, "At least one expressions to delete is required.");

			this.deleteList = Arrays.asList(expressions);
		}

		@Override
		public OngoingMatchAndReturn returning(Expression... expressions) {

			Assert.notNull(expressions, "Expressions to return are required.");
			Assert.notEmpty(expressions, "At least one expressions to return is required.");

			super.returnList.addAll(Arrays.asList(expressions));
			return this;
		}

		@Override
		public OngoingMatchAndReturn returningDistinct(Expression... expressions) {

			returning(expressions);
			super.distinct = true;
			return this;
		}

		@Override
		public Statement build() {

			DefaultStatementBuilder.this.addUpdatingClause(buildDelete());
			return super.build();
		}

		@Override
		public OngoingMatchAndDelete delete(Expression... expressions) {
			return delete(false, expressions);
		}

		@Override
		public OngoingMatchAndDelete detachDelete(Expression... expressions) {
			return delete(true, expressions);
		}

		private OngoingMatchAndDelete delete(boolean nextDetach, Expression... expressions) {
			DefaultStatementBuilder.this.addUpdatingClause(buildDelete());
			return DefaultStatementBuilder.this.new DefaultStatementWithDeleteBuilder(nextDetach, expressions);
		}

		@Override
		public OngoingMatchAndWithWithoutWhere with(Expression... expressions) {
			return this.with(false, expressions);
		}

		@Override
		public OngoingMatchAndWithWithoutWhere withDistinct(Expression... expressions) {
			return this.with(true, expressions);
		}

		private OngoingMatchAndWithWithoutWhere with(boolean distinct, Expression... expressions) {
			DefaultStatementBuilder.this.addUpdatingClause(buildDelete());
			return DefaultStatementBuilder.this
				.addWith(buildWith())
				.with(distinct, expressions);
		}

		private Delete buildDelete() {

			ExpressionList deleteItems = new ExpressionList(this.deleteList);
			return new Delete(deleteItems, this.detach);
		}
	}

	// Static builder and support classes

	static final class DefaultMatchBuilder {

		private final List<PatternElement> matchList = new ArrayList<>();

		private final DefaultConditionBuilder conditionBuilder = new DefaultConditionBuilder();

		private final boolean optional;

		DefaultMatchBuilder(boolean optional) {
			this.optional = optional;
		}

		Match buildMatch() {
			Pattern pattern = new Pattern(this.matchList);
			return new Match(optional, pattern, conditionBuilder.buildCondition().map(Where::new).orElse(null));
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
