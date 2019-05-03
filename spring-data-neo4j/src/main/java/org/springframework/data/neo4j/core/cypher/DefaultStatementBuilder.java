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

import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatch;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchAndReturn;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchWithWhere;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchWithoutWhere;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingOrderDefinition;
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
	OngoingMatchWithoutWhere,
	OngoingMatchAndReturn,
	OngoingOrderDefinition, StatementBuilder.OngoingMatchAndReturnWithOrder {

	private List<PatternElement> matchList = new ArrayList<>();
	private List<Expression> returnList = new ArrayList<>();
	private List<SortItem> sortItemList = new ArrayList<>();
	private SortItem lastSortItem;
	private Skip skip;
	private Limit limit;
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

		Assert.notNull(expressions, "Expressions to return are required.");
		Assert.notEmpty(expressions, "At least one expressions to return is required.");

		this.returnList.addAll(Arrays.asList(expressions));
		return this;
	}

	@Override
	public OngoingMatchAndReturn orderBy(SortItem... sortItem) {
		Arrays.stream(sortItem).forEach(this.sortItemList::add);
		return this;
	}

	@Override
	public OngoingOrderDefinition orderBy(Expression expression) {
		this.lastSortItem = Cypher.sort(expression);
		return this;

	}

	@Override
	public OngoingMatchAndReturn descending() {
		this.sortItemList.add(this.lastSortItem.descending());
		this.lastSortItem = null;
		return this;
	}

	@Override
	public OngoingMatchAndReturn ascending() {
		this.sortItemList.add(this.lastSortItem.ascending());
		this.lastSortItem = null;
		return this;
	}

	@Override
	public OngoingOrderDefinition and(Expression expression) {
		return orderBy(expression);
	}

	@Override
	public OngoingMatchAndReturn skip(Number number) {
		skip = Skip.of(number);
		return this;
	}

	@Override
	public OngoingMatchAndReturn limit(Number number) {
		limit = Limit.of(number);
		return this;
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

	@Override
	public Statement build() {

		Pattern pattern = new Pattern(this.matchList);
		Match match = new Match(pattern, hasCondition() ? new Where(this.condition) : null);
		ExpressionList returnItems = new ExpressionList(this.returnList);

		if (lastSortItem != null) {
			sortItemList.add(lastSortItem);
		}
		Order order = sortItemList.size() > 0 ? new Order(sortItemList) : null;

		return new SinglePartQuery(match, new Return(returnItems, order, skip, limit));
	}

	private boolean hasCondition() {
		return !(this.condition == null || this.condition == CompoundCondition.EMPTY_CONDITION);
	}
}
