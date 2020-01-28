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

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * Builder for various conditions. Used internally from properties and other expressions that should take part in
 * conditions.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Conditions {

	/**
	 * Creates a condition that matches if the right hand side is a regular expression that matches the the left hand side via
	 * {@code =~}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return A "matches" comparision
	 */
	static Condition matches(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.MATCHES, rhs);
	}

	/**
	 * Creates a condition that matches if both expressions are equals according to {@code =}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return An "equals" comparision
	 */
	static Condition isEqualTo(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.EQUALITY, rhs);
	}

	/**
	 * Creates a condition that matches if both expressions are equals according to {@code <>}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return An "not equals" comparision
	 */
	static Condition isNotEqualTo(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.INEQUALITY, rhs);
	}

	/**
	 * Creates a condition that matches if the left hand side is less than the right hand side..
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return An "less than" comparision
	 */
	static Condition lt(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.LESS_THAN, rhs);
	}

	/**
	 * Creates a condition that matches if the left hand side is less than or equal the right hand side..
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return An "less than or equal" comparision
	 */
	static Condition lte(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.LESS_THAN_OR_EQUAL_TO, rhs);
	}

	/**
	 * Creates a condition that matches if the left hand side is greater than or equal the right hand side..
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return An "greater than or equal" comparision
	 */
	static Condition gte(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.GREATER_THAN_OR_EQUAL_TO, rhs);
	}

	/**
	 * Creates a condition that matches if the left hand side is greater than the right hand side..
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return An "greater than" comparision
	 */
	static Condition gt(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.GREATER_THAN, rhs);
	}

	/**
	 * Negates the given condition.
	 *
	 * @param condition The condition to negate. Must not be null.
	 * @return The negated condition.
	 */
	static Condition not(Condition condition) {

		Assert.notNull(condition, "Condition to negate must not be null.");
		return condition.not();
	}

	/**
	 * Creates a condition that checks whether the {@code lhs} starts with the {@code rhs}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return A new condition.
	 */
	static Condition startsWith(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.STARTS_WITH, rhs);
	}

	/**
	 * Creates a condition that checks whether the {@code lhs} contains with the {@code rhs}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return A new condition.
	 */
	static Condition contains(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.CONTAINS, rhs);
	}

	/**
	 * Creates a condition that checks whether the {@code lhs} ends with the {@code rhs}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparision
	 * @return A new condition.
	 */
	static Condition endsWith(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, Operator.ENDS_WITH, rhs);
	}

	/**
	 * Creates a placeholder condition which is not rendered in the final statement but is useful while chaining
	 * conditions together.
	 *
	 * @return A placeholder condition.
	 */
	public static Condition noCondition() {

		return CompoundCondition.empty();
	}

	/**
	 * Creates a condition that checks whether the {@code expression} is {@literal null}.
	 *
	 * @param expression The expression to check for {@literal null}
	 * @return A new condition.
	 */
	static Condition isNull(Expression expression) {

		return Comparison.create(Operator.IS_NULL, expression);
	}

	/**
	 * Creates a condition that checks whether the {@code expression} is not {@literal null}.
	 *
	 * @param expression The expression to check for {@literal null}
	 * @return A new condition.
	 */
	static Condition isNotNull(Expression expression) {

		return Comparison.create(Operator.IS_NOT_NULL, expression);
	}

	/**
	 * A condition that evaluates to true if a list or a string represented by {@code expression} is empty or has the length of 0.
	 *
	 * @param expression The expression to test for emptiness.
	 * @return A new condition.
	 */
	static Condition isEmpty(Expression expression) {

		return Functions.size(expression).isEqualTo(Cypher.literalOf(0L));
	}

	/**
	 * A condition that evaluates to true if the expression {@code expression} exists.
	 *
	 * @param expression The expression to check whether it exists or not
	 * @return A new condition.
	 */
	public static Condition exists(Expression expression) {

		return new BooleanFunctionCondition(Functions.exists(expression));
	}

	/**
	 * Not to be instantiated.
	 */
	private Conditions() {
	}
}
