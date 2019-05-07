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

	private List<PatternElement> matchList = new ArrayList<>();

	private Condition condition;

	@Override
	public OngoingMatchWithoutWhere match(PatternElement... pattern) {

		Assert.notNull(pattern, "Patterns to match are required.");
		Assert.notEmpty(pattern, "At least one pattern to match is required.");

		this.matchList.addAll(Arrays.asList(pattern));
		return this;
	}

	@Override
	public OngoingMatchAndReturn returning(Expression... expressions) {

		DefaultStatementWithReturnBuilder ongoingMatchAndReturn = new DefaultStatementWithReturnBuilder();
		ongoingMatchAndReturn.addExpressions(expressions);
		return ongoingMatchAndReturn;
	}

	@Override
	public OngoingDetachDelete detach() {

		return new DefaultStatementWithDeleteBuilder(true);
	}

	@Override
	public OngoingMatchAndDelete delete(Expression... expressions) {

		return new DefaultStatementWithDeleteBuilder(false).delete(expressions);
	}

	@Override
	public OngoingMatchWithWhere where(Condition newCondition) {

		this.condition = newCondition;
		return this;
	}

	@Override
	public OngoingMatchWithWhere and(Condition additionalCondition) {

		this.condition = this.condition.and(additionalCondition);
		return this;
	}

	@Override
	public OngoingMatchWithWhere or(Condition additionalCondition) {

		this.condition = this.condition.or(additionalCondition);
		return this;
	}

	private boolean hasCondition() {
		return !(this.condition == null || this.condition == CompoundCondition.EMPTY_CONDITION);
	}

	protected Match buildMatch() {
		Pattern pattern = new Pattern(DefaultStatementBuilder.this.matchList);
		return new Match(pattern, hasCondition() ? new Where(DefaultStatementBuilder.this.condition) : null);
	}

	class DefaultStatementWithReturnBuilder
		implements OngoingMatchAndReturn, OngoingOrderDefinition, OngoingMatchAndReturnWithOrder {

		private final List<Expression> returnList = new ArrayList<>();
		private final List<SortItem> sortItemList = new ArrayList<>();
		private SortItem lastSortItem;
		private Skip skip;
		private Limit limit;

		protected final DefaultStatementWithReturnBuilder addExpressions(Expression... expressions) {

			Assert.notNull(expressions, "Expressions to return are required.");
			Assert.notEmpty(expressions, "At least one expressions to return is required.");

			this.returnList.addAll(Arrays.asList(expressions));
			return this;
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
		public final OngoingMatchAndReturn skip(Number number) {
			skip = Skip.of(number);
			return this;
		}

		@Override
		public final OngoingMatchAndReturn limit(Number number) {
			limit = Limit.of(number);
			return this;
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
			return Optional.of(new Return(returnItems, order, skip, limit));
		}

		@Override
		public Statement build() {

			Match match = buildMatch();
			// This must be filled at this stage
			Return aReturn = buildReturn().get();
			return SinglePartQuery.createReturningQuery(aReturn, match);
		}
	}

	class DefaultStatementWithDeleteBuilder extends DefaultStatementWithReturnBuilder
		implements OngoingDetachDelete, OngoingMatchAndDelete {

		private final List<Expression> deleteList = new ArrayList<>();
		private final boolean detach;

		DefaultStatementWithDeleteBuilder(boolean detach) {
			this.detach = detach;
		}

		@Override
		public OngoingMatchAndDelete delete(Expression... expressions) {

			Assert.notNull(expressions, "Expressions to delete are required.");
			Assert.notEmpty(expressions, "At least one expressions to delete is required.");

			this.deleteList.addAll(Arrays.asList(expressions));

			return this;
		}

		@Override
		public OngoingMatchAndReturn returning(Expression... expressions) {


			Assert.notNull(expressions, "Expressions to return are required.");
			Assert.notEmpty(expressions, "At least one expressions to return is required.");

			super.returnList.addAll(Arrays.asList(expressions));
			return this;
		}

		protected final Delete buildDelete() {

			ExpressionList deleteItems = new ExpressionList(this.deleteList);
			return new Delete(deleteItems, this.detach);
		}

		@Override
		public Statement build() {

			Match match = buildMatch();
			Delete delete = buildDelete();
			Optional<Return> optionalReturn = buildReturn();

			return SinglePartQuery.createUpdatingQuery(match, delete, optionalReturn.orElse(null));
		}
	}
}
