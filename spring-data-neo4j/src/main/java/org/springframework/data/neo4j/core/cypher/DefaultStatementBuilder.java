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

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatch;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchAndReturn;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
class DefaultStatementBuilder
	implements StatementBuilder, OngoingMatch, OngoingMatchAndReturn {

	private List<PatternElement> matchList = new ArrayList<>();
	private List<Expression> returnList = new ArrayList<>();
	private Condition where;

	@Override
	public OngoingMatch match(PatternElement... pattern) {

		Assert.notNull(pattern, "Patterns to match are required.");
		Assert.notEmpty(pattern, "At least one pattern to match is required.");

		this.matchList.addAll(Arrays.asList(pattern));
		return this;
	}

	@Override
	public OngoingMatchAndReturn returning(Expression... expressions) {

		Assert.notNull(expressions, "Expressions to return are required.");
		Assert.notEmpty(expressions, "At least one expressions to return is required.");

		this.returnList.addAll(Arrays.stream(expressions)
			.map(expression -> expression instanceof Named ?
				((Named) expression).getSymbolicName().map(Expression.class::cast).orElse(expression) :
				expression)
			.collect(toList()));
		return this;
	}

	@Override
	public OngoingMatchAndReturn returning(Node... nodes) {

		Assert.notNull(nodes, "Nodes to return are required.");
		Assert.notEmpty(nodes, "At least one node to return is required.");

		this.returnList.addAll(Arrays.stream(nodes)
			.map(node -> node.getSymbolicName().map(Expression.class::cast).orElse(node))
			.collect(toList()));
		return this;
	}

	@Override
	public OngoingMatch where(Condition condition) {

		this.where = condition;
		return this;
	}

	@Override
	public Statement build() {

		Pattern pattern = new Pattern(this.matchList);
		Match match = new Match(pattern, this.where == null ? null : new Where(this.where));
		return new SinglePartQuery(match, new Return(returnList.stream().map(ReturnItem::new).collect(toList())));
	}
}
