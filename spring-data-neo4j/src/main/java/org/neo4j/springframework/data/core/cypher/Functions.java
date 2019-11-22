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
package org.neo4j.springframework.data.core.cypher;

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * Factory methods for creating instances of {@link FunctionInvocation functions}.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
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

		return new FunctionInvocation(F_ID, node);
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
		Assert.isTrue(relationship.getSymbolicName().isPresent(), "The relationship needs to be named.");

		return new FunctionInvocation(F_ID, relationship);
	}

	public static FunctionInvocation id(SymbolicName symbolicName) {

		Assert.notNull(symbolicName, "The symbolic name is required.");

		return new FunctionInvocation(F_ID, symbolicName);
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
		Assert.isTrue(relationship.getSymbolicName().isPresent(), "The relationship needs to be named.");

		return new FunctionInvocation("type", relationship);
	}

	/**
	 * Creates a function invocation for the {@code count()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/aggregating/#functions-count">count</a>.
	 *
	 * @param expression An expression describing the things to count. Can be a node, relationship, alias or
	 *                   a symbolic name
	 * @return A function call for {@code count()} for one  expression
	 */
	public static FunctionInvocation count(Expression expression) {

		Assert.notNull(expression, "The expression to count is required.");

		return new FunctionInvocation("count", expression);
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
	 * <li><a href="https://neo4j.com/docs/cypher-manual/current/functions/scalar/#functions-size-of-pattern-expression">to a pattern expression</a></li>
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

	private Functions() {
	}
}
