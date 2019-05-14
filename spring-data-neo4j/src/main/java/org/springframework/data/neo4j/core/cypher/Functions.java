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

		return new FunctionInvocation(F_ID, relationship);
	}

	public static FunctionInvocation id(SymbolicName symbolicName) {

		Assert.notNull(symbolicName, "The symbolic name is required.");

		return new FunctionInvocation(F_ID, symbolicName);
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
	 * Creates a function invocation for the {@code } function.
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

	private Functions() {
	}
}
