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

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * Represents a list expression in contrast to an {@link ExpressionList} which is a list of expressions. The list expression
 * here uses an expression list for it's content.
 *
 * @author Michael J. Simons
 * @soundtrack Queen - Jazz
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class ListExpression implements Expression {

	static Expression listOrSingleExpression(Expression... expressions) {

		Assert.notNull(expressions, "Expressions are required.");
		Assert.notEmpty(expressions, "At least one expression is required.");

		if (expressions.length == 1) {
			return expressions[0];
		} else {
			return ListExpression.create(expressions);
		}
	}

	static ListExpression create(Expression... expressions) {

		return new ListExpression(new ExpressionList<>(expressions));
	}

	private final ExpressionList<?> content;

	private ListExpression(ExpressionList<?> content) {
		this.content = content;
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		this.content.accept(visitor);
		visitor.leave(this);
	}
}
