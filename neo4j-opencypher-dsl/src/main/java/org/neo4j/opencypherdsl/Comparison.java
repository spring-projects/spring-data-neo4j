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
package org.neo4j.opencypherdsl;

import static org.apiguardian.api.API.Status.*;
import static org.neo4j.opencypherdsl.Expressions.*;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * A concrete condition representing a comparision between two expressions.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Comparison implements Condition {

	static Comparison create(Operator operator, Expression expression) {

		Assert.isTrue(operator.isUnary(), "Operator must be unary.");
		Assert.notNull(expression, "Expression must not be null.");

		switch (operator.getType()) {
			case PREFIX:
				return new Comparison(null, operator, expression);
			case POSTFIX:
				return new Comparison(expression, operator, null);
			default:
				throw new IllegalArgumentException("Invalid operator type " + operator.getType());
		}
	}

	static Comparison create(Expression lhs, Operator operator, Expression rhs) {

		Assert.notNull(lhs, "Left expression must not be null.");
		Assert.notNull(operator, "Operator must not be empty.");
		Assert.notNull(rhs, "Right expression must not be null.");

		return new Comparison(lhs, operator, rhs);
	}

	private static Expression nestedIfCondition(Expression expression) {
		return expression instanceof Condition ? new NestedExpression(expression) : expression;
	}

	private final Expression left;
	private final Operator comparator;
	private final Expression right;

	private Comparison(Expression left, Operator operator, Expression right) {

		this.left = nestedIfCondition(left);
		this.comparator = operator;
		this.right = nestedIfCondition(right);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		if (left != null) {
			nameOrExpression(left).accept(visitor);
		}
		comparator.accept(visitor);
		if (right != null) {
			nameOrExpression(right).accept(visitor);
		}
		visitor.leave(this);
	}
}

