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

import java.util.Collections;

import org.apiguardian.api.API;

/**
 * Factory methods for creating instances of {@link FunctionInvocation functions}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Romain Rossi
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Functions {

	private static final String F_ID = "id";

	/**
	 * Creates a function invocation for {@code id{}}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-id">id</a>.
	 *
	 * @param node The node for which the internal id should be retrieved
	 * @return A function call for {@code id()} on a node.
	 */
	public static FunctionInvocation id(Node node) {

		Assert.notNull(node, "The node parameter is required.");

		return new FunctionInvocation(F_ID, node.getRequiredSymbolicName());
	}

	/**
	 * Creates a function invocation for {@code id{}}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-id">id</a>.
	 *
	 * @param relationship The relationship for which the internal id should be retrieved
	 * @return A function call for {@code id()} on a relationship.
	 */
	public static FunctionInvocation id(Relationship relationship) {

		Assert.notNull(relationship, "The relationship parameter is required.");

		return new FunctionInvocation(F_ID, relationship.getRequiredSymbolicName());
	}

	/**
	 * Creates a function invocation for {@code labels{}}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/list/#functions-labels">labels</a>.
	 *
	 * @param node The node for which the labels should be retrieved
	 * @return A function call for {@code labels()} on a node.
	 */
	public static FunctionInvocation labels(Node node) {

		Assert.notNull(node, "The node parameter is required.");

		return new FunctionInvocation("labels", node.getRequiredSymbolicName());
	}

	/**
	 * Creates a function invocation for {@code type{}}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-type">type</a>.
	 *
	 * @param relationship The relationship for which the type should be retrieved
	 * @return A function call for {@code type()} on a relationship.
	 */
	public static FunctionInvocation type(Relationship relationship) {

		Assert.notNull(relationship, "The relationship parameter is required.");

		return new FunctionInvocation("type", relationship.getRequiredSymbolicName());
	}

	/**
	 * @param node The named node to be counted
	 * @return A function call for {@code count()} for one named node
	 * @see #count(Expression)
	 */
	public static FunctionInvocation count(Node node) {

		Assert.notNull(node, "The node parameter is required.");

		return count(node.getRequiredSymbolicName());
	}

	/**
	 * Creates a function invocation for the {@code count()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-count">count</a>.
	 *
	 * @param expression An expression describing the things to count.
	 * @return A function call for {@code count()} for an expression like {@link Cypher#asterisk()} etc.
	 */
	public static FunctionInvocation count(Expression expression) {

		Assert.notNull(expression, "The expression to count is required.");

		return new FunctionInvocation("count", expression);
	}

	/**
	 * Creates a function invocation for a {@code count()} function with {@code DISTINCT} added.
	 *
	 * @param node The named node to be counted
	 * @return A function call for {@code count()} for one named node
	 * @see #countDistinct(Expression)
	 */
	public static FunctionInvocation countDistinct(Node node) {

		Assert.notNull(node, "The node parameter is required.");

		return countDistinct(node.getRequiredSymbolicName());
	}

	/**
	 * Creates a function invocation for a {@code count()} function with {@code DISTINCT} added.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-count">count</a>.
	 *
	 * @param expression An expression describing the things to count.
	 * @return A function call for {@code count()} for an expression like {@link Cypher#asterisk()} etc.
	 */
	public static FunctionInvocation countDistinct(Expression expression) {

		Assert.notNull(expression, "The expression to count is required.");

		return new FunctionInvocation("count", new DistinctExpression(expression));
	}

	/**
	 * Creates a function invocation for the {@code coalesce()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-coalesce">coalesce</a>.
	 *
	 * @param expressions One or more expressions to be coalesced
	 * @return A function call for {@code coalesce}.
	 */
	public static FunctionInvocation coalesce(Expression... expressions) {

		Assert.notEmpty(expressions, "At least one expression is required.");
		Assert.notNull(expressions[0], "At least one expression is required.");

		return new FunctionInvocation("coalesce", expressions);
	}

	/**
	 * Creates a function invocation for the {@code toLower()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/string/#functions-toLower">toLower</a>.
	 *
	 * @param expression An expression resolving to a string
	 * @return A function call for {@code toLower()} for one expression
	 */
	public static FunctionInvocation toLower(Expression expression) {

		Assert.notNull(expression, "The expression for toLower() is required.");

		return new FunctionInvocation("toLower", expression);
	}

	/**
	 * Creates a function invocation for the {@code size()} function. {@code size} can be applied to
	 * <ul>
	 * <li><a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-size">a list</a></li>
	 * <li><a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-size-of-string">to a string</a></li>
	 * </ul>
	 *
	 * @param expression The expression who's size is to be returned
	 * @return A function call for {@code size()} for one expression
	 */
	public static FunctionInvocation size(Expression expression) {

		Assert.notNull(expression, "The expression for size() is required.");

		return new FunctionInvocation("size", expression);
	}

	/**
	 * Creates a function invocation for the {@code size()} function. {@code size} can be applied to
	 * <ul>
	 * <li><a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-size-of-pattern-expression">to a pattern expression</a></li>
	 * </ul>
	 *
	 * @param pattern The pattern for which {@code size()} should be invoked.
	 * @return A function call for {@code size()} for a pattern
	 */
	public static FunctionInvocation size(RelationshipPattern pattern) {

		Assert.notNull(pattern, "The pattern for size() is required.");

		return new FunctionInvocation("size", new Pattern(Collections.singletonList(pattern)));
	}

	/**
	 * Creates a function invocation for the {@code exists()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-exists">exists</a>.
	 *
	 * @param expression The expression who's existence is to be evaluated
	 * @return A function call for {@code exists()} for one expression
	 */
	public static FunctionInvocation exists(Expression expression) {

		Assert.notNull(expression, "The expression for exists() is required.");

		return new FunctionInvocation("exists", expression);
	}

	/**
	 * Creates a function invocation for the {@code distance()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/spatial/#functions-distance">exists</a>.
	 * Both points need to be in the same coordinate system.
	 *
	 * @param point1 Point 1
	 * @param point2 Point 2
	 * @return A function call for {@code distance()}
	 */
	public static FunctionInvocation distance(Expression point1, Expression point2) {

		Assert.notNull(point1, "The distance function requires two points.");
		Assert.notNull(point2, "The distance function requires two points.");

		return new FunctionInvocation("distance", point1, point2);
	}

	/**
	 * Creates a function invocation for the {@code point()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/spatial/#functions-point">point</a>.
	 *
	 * @param parameterMap The map of parameters for {@code point()}
	 * @return A function call for {@code point()}
	 */
	public static FunctionInvocation point(MapExpression parameterMap) {

		Assert.notNull(parameterMap, "The parameter map is required.");

		return new FunctionInvocation("point", parameterMap);
	}

	/**
	 * Creates a function invocation for the {@code avg()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-avg">avg</a>.
	 *
	 * @param expression The things to average
	 * @return A function call for {@code avg()}
	 */
	public static FunctionInvocation avg(Expression expression) {

		Assert.notNull(expression, "The expression to average is required.");

		return new FunctionInvocation("avg", expression);
	}

	/**
	 * @param variable The named thing to collect
	 * @return A function call for {@code collect()}
	 * @see #collect(Expression)
	 */
	public static FunctionInvocation collect(Named variable) {

		Assert.notNull(variable, "The variable parameter is required.");

		return Functions.collect(variable.getRequiredSymbolicName());
	}

	/**
	 * Creates a function invocation for the {@code collect()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-collect">collect</a>.
	 *
	 * @param expression The things to collect
	 * @return A function call for {@code collect()}
	 */
	public static FunctionInvocation collect(Expression expression) {

		Assert.notNull(expression, "The expression to collect is required.");

		return new FunctionInvocation("collect", expression);
	}

	/**
	 * Creates a function invocation for the {@code max()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-max">max</a>.
	 *
	 * @param expression A list from which the maximum element value is returned
	 * @return A function call for {@code max()}
	 */
	public static FunctionInvocation max(Expression expression) {

		Assert.notNull(expression, "The expression for max is required.");

		return new FunctionInvocation("max", expression);
	}

	/**
	 * Creates a function invocation for the {@code min()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-min">min</a>.
	 *
	 * @param expression A list from which the minimum element value is returned
	 * @return A function call for {@code min()}
	 */
	public static FunctionInvocation min(Expression expression) {

		Assert.notNull(expression, "The expression for min is required.");

		return new FunctionInvocation("min", expression);
	}

	/**
	 * Creates a function invocation for the {@code percentileCont()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-percentilecont">percentileCont</a>.
	 *
	 * @param expression A numeric expression
	 * @param percentile A numeric value between 0.0 and 1.0
	 * @return A function call for {@code percentileCont()}
	 */
	public static FunctionInvocation percentileCont(Expression expression, Number percentile) {

		Assert.notNull(expression, "The numeric expression for percentileCont is required.");
		Assert.notNull(percentile, "The percentile for percentileCont is required.");
		final double p = percentile.doubleValue();
		Assert.isTrue(p >= 0D && p <= 1D, "The percentile for percentileCont must be between 0.0 and 1.0.");

		return new FunctionInvocation("percentileCont", expression, new NumberLiteral(percentile));
	}

	/**
	 * Creates a function invocation for the {@code percentileDisc()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-percentiledisc">percentileDisc</a>.
	 *
	 * @param expression A numeric expression
	 * @param percentile A numeric value between 0.0 and 1.0
	 * @return A function call for {@code percentileDisc()}
	 */
	public static FunctionInvocation percentileDisc(Expression expression, Number percentile) {

		Assert.notNull(expression, "The numeric expression for percentileDisc is required.");
		Assert.notNull(percentile, "The percentile for percentileDisc is required.");
		final double p = percentile.doubleValue();
		Assert.isTrue(p >= 0D && p <= 1D, "The percentile for percentileDisc must be between 0.0 and 1.0.");

		return new FunctionInvocation("percentileDisc", expression, new NumberLiteral(percentile));
	}

	/**
	 * Creates a function invocation for the {@code stDev()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-stdev">stDev</a>.
	 *
	 * @param expression A numeric expression
	 * @return A function call for {@code stDev()}
	 */
	public static FunctionInvocation stDev(Expression expression) {

		Assert.notNull(expression, "The numeric expression for stDev is required.");

		return new FunctionInvocation("stDev", expression);
	}

	/**
	 * Creates a function invocation for the {@code stDevP()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-stdevp">stDevP</a>.
	 *
	 * @param expression A numeric expression
	 * @return A function call for {@code stDevP()}
	 */
	public static FunctionInvocation stDevP(Expression expression) {

		Assert.notNull(expression, "The numeric expression for stDevP is required.");

		return new FunctionInvocation("stDevP", expression);
	}

	/**
	 * Creates a function invocation for the {@code sum()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-sum">sum</a>.
	 *
	 * @param expression An expression returning a set of numeric values
	 * @return A function call for {@code sum()}
	 */
	public static FunctionInvocation sum(Expression expression) {

		Assert.notNull(expression, "The set of numeric expression for sum is required.");

		return new FunctionInvocation("sum", expression);
	}

	/**
	 * @param start the range's start
	 * @param end   the range's end
	 * @return A function call for {@code range()}
	 * @see #range(Expression, Expression, Expression)
	 */
	public static FunctionInvocation range(Expression start, Expression end) {
		return range(start, end, null);
	}

	/**
	 * Creates a function invocation for the {@code range()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/list/#functions-range">range</a>.
	 *
	 * @param start the range's start
	 * @param end   the range's end
	 * @param step  the range's step
	 * @return A function call for {@code range()}
	 */
	public static FunctionInvocation range(Expression start, Expression end, Expression step) {

		Assert.notNull(start, "The expression for range is required.");
		Assert.notNull(end, "The expression for range is required.");

		if (step == null) {
			return new FunctionInvocation("range", start, end);
		} else {
			return new FunctionInvocation("range", start, end, step);
		}
	}

	/**
	 * Creates a function invocation for the {@code head()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-head">head</a>.
	 *
	 * @param expression A list from which the head element is returned
	 * @return A function call for {@code head()}
	 */
	public static FunctionInvocation head(Expression expression) {

		Assert.notNull(expression, "The expression for head is required.");

		return new FunctionInvocation("head", expression);
	}

	/**
	 * Creates a function invocation for the {@code last()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-last">last</a>.
	 *
	 * @param expression A list from which the last element is returned
	 * @return A function call for {@code last()}
	 */
	public static FunctionInvocation last(Expression expression) {

		Assert.notNull(expression, "The expression for last is required.");

		return new FunctionInvocation("last", expression);
	}

	/**
	 * Creates a function invocation for {@code nodes{}}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/list/#functions-nodes">labels</a>.
	 *
	 * @param path The path for which the number of nodes should be retrieved
	 * @return A function call for {@code nodes()} on a node.
	 * @since 1.1
	 */
	public static FunctionInvocation nodes(NamedPath path) {

		Assert.notNull(path, "The path for nodes is required.");
		return path.getSymbolicName().map(n -> new FunctionInvocation("nodes", n))
			.orElseThrow(() -> new IllegalArgumentException("The path needs to be named!"));
	}

	private Functions() {
	}
}
