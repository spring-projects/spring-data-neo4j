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

import static org.apiguardian.api.API.Status.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.springframework.util.Assert;

/**
 * An expression can be used in many places, i.e. in return statements, pattern elements etc.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public interface Expression extends Visitable {

	/**
	 * Creates an expression with an alias. This expression does not track which or how many aliases have been created.
	 *
	 * @param alias The alias to use
	 * @return An aliased expression.
	 */
	default AliasedExpression as(String alias) {

		Assert.hasText(alias, "The alias may not be null or empty.");
		return new AliasedExpression(this, alias);
	}

	default Condition isEqualTo(Expression rhs) {
		return Conditions.isEqualTo(this, rhs);
	}

	default Condition isNotEqualTo(Expression rhs) {
		return Conditions.isNotEqualTo(this, rhs);
	}

	default Condition lt(Expression rhs) {
		return Conditions.lt(this, rhs);
	}

	default Condition lte(Expression rhs) {
		return Conditions.lte(this, rhs);
	}

	default Condition gt(Expression rhs) {
		return Conditions.gt(this, rhs);
	}

	default Condition gte(Expression rhs) {
		return Conditions.gte(this, rhs);
	}

	/**
	 * Creates a condition that checks whether this {@code expression} is {@literal true}.
	 *
	 * @return A new condition
	 */
	default Condition isTrue() {
		return Conditions.isEqualTo(this, literalTrue());
	}

	/**
	 * Creates a condition that checks whether this {@code expression} is {@literal false}.
	 *
	 * @return A new condition
	 */
	default Condition isFalse() {
		return Conditions.isEqualTo(this, literalFalse());
	}

	/**
	 * Creates a condition that checks whether this {@code expression} matches that {@code expression}.
	 *
	 * @param expression The expression to match against. Must evaluate into a string during runtime.
	 * @return A new condition.
	 */
	default Condition matches(Expression expression) {
		return Conditions.matches(this, expression);
	}

	/**
	 * Creates a condition that checks whether this {@code expression} matches the given {@code pattern}.
	 *
	 * @param pattern The pattern to match
	 * @return A new condition.
	 */
	default Condition matches(String pattern) {
		return Conditions.matches(this, Cypher.literalOf(pattern));
	}

	/**
	 * Creates a condition that checks whether this {@code expression} starts with that {@code expression}.
	 *
	 * @param expression The expression to match against. Must evaluate into a string during runtime.
	 * @return A new condition.
	 */
	default Condition startsWith(Expression expression) {
		return Conditions.startsWith(this, expression);
	}

	/**
	 * Creates a condition that checks whether this {@code expression} contains that {@code expression}.
	 *
	 * @param expression The expression to match against. Must evaluate into a string during runtime.
	 * @return A new condition.
	 */
	default Condition contains(Expression expression) {
		return Conditions.contains(this, expression);
	}

	/**
	 * Creates a condition that checks whether this {@code expression} ends with that {@code expression}.
	 *
	 * @param expression The expression to match against. Must evaluate into a string during runtime.
	 * @return A new condition.
	 */
	default Condition endsWith(Expression expression) {
		return Conditions.endsWith(this, expression);
	}

	/**
	 * Creates an expression concatenating two string or list expressions.
	 *
	 * @param expression The expression to concat to this expression.
	 * @return A new expression.
	 */
	default Operation concat(Expression expression) {
		return Operations.concat(this, expression);
	}

	/**
	 * Creates a {@code +} operation of this (the augend) and the {@code addend}.
	 *
	 * @param addend The addend
	 * @return A new operation.
	 * @since 1.0.1
	 */
	default Operation add(Expression addend) {
		return Operations.add(this, addend);
	}

	/**
	 * Creates a {@code -} operation of this (the minuend) and the {@code subtrahend}.
	 *
	 * @param subtrahend The subtrahend
	 * @return A new operation.
	 * @since 1.0.1
	 */
	default Operation subtract(Expression subtrahend) {
		return Operations.subtract(this, subtrahend);
	}

	/**
	 * Creates a {@code *} operation of this (the multiplier) and the {@code multiplicand}.
	 *
	 * @param multiplicand The multiplicand
	 * @return A new operation.
	 * @since 1.0.1
	 */
	default Operation multiply(Expression multiplicand) {
		return Operations.multiply(this, multiplicand);
	}

	/**
	 * Creates a {@code /} operation of this (the divisor) and the {@code dividend}.
	 *
	 * @param dividend The dividend
	 * @return A new operation.
	 * @since 1.0.1
	 */
	default Operation divide(Expression dividend) {
		return Operations.divide(this, dividend);
	}

	/**
	 * Returns the remainder of this value and the {@code dividend}.
	 *
	 * @param dividend The dividend
	 * @return A new operation.
	 */
	default Operation remainder(Expression dividend) {
		return Operations.remainder(this, dividend);
	}

	/**
	 * Returns the power of n of this value.
	 *
	 * @param n power to raise this {@code Expression} to.
	 * @return A new operation.
	 */
	default Operation pow(Expression n) {

		return Operations.pow(this, n);
	}

	/**
	 * Creates a {@code IS NULL} operation for this {@code expression}.
	 * The expression does not track the condition created here.
	 *
	 * @return A condition based on this expression that evaluates to true when this expression is null.
	 */
	default Condition isNull() {
		return Conditions.isNull(this);
	}

	/**
	 * Creates a {@code IS NOT NULL} operation for this {@code expression}.
	 * The expression does not track the condition created here.
	 *
	 * @return A condition based on this expression that evaluates to true when this expression is not null.
	 */
	default Condition isNotNull() {
		return Conditions.isNotNull(this);
	}

	/**
	 * Creates a {@code IN} operation for this expression and that {@code expression}.
	 * The expression does not track the condition created here.
	 *
	 * @param haystack The expression to search for this expression
	 * @return A new condition.
	 */
	default Condition in(Expression haystack) {
		return Comparison.create(this, Operator.IN, haystack);
	}

	/**
	 * Creates a condition that evaluates to true if this expression is empty.
	 *
	 * @return A new condition.
	 */
	default Condition isEmpty() {
		return Conditions.isEmpty(this);
	}

	/**
	 * The property does not track the sort items created here.
	 *
	 * @return A sort item for this property in descending order
	 */
	default SortItem descending() {

		return SortItem.create(this, SortItem.Direction.DESC);
	}

	/**
	 * The property does not track the sort items created here.
	 *
	 * @return A sort item for this property in ascending order
	 */
	default SortItem ascending() {

		return SortItem.create(this, SortItem.Direction.ASC);
	}
}
