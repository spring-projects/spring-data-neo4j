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

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * A condition to check whether an expression is null. By negating it effectively becomes {@literal IsNotNull}.
 * This condition can either be negated itself or through a {@link NotCondition}, which is the same style
 * that Cypher supports as well.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class IsNull implements Condition {

	/**
	 * Creates a new {@link IsNull IsNull-condition}.
	 *
	 * @param expression The expression to check for null, must not be {@literal null}
	 * @param negated A flag whether to turn this into a {@literal IsNotNull}
	 * @return A new condition
	 */
	static IsNull create(Expression expression, boolean negated) {

		Assert.notNull(expression, "Expression must not be null.");

		return new IsNull(expression, negated);
	}

	/** The expression to check for null, must not be be {@literal null}. */
	private final Expression expression;

	/**
	 * Flag, if this is actually a IsNotNull.
	 */
	private final boolean negated;

	private IsNull(Expression expression, boolean negated) {

		this.expression = expression;
		this.negated = negated;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		expression.accept(visitor);
		visitor.leave(this);
	}
}
