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
import org.springframework.data.neo4j.core.cypher.StatementBuilder.ExposesMatch;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchWithoutWhere;
import org.springframework.lang.Nullable;

/**
 * The main entry point into the Cypher DSL.
 *
 * The Cypher Builder API is intended for framework usage to produce Cypher statements required for database operations.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Cypher {

	/**
	 * Create a new Node representation with at least one label, the "primary" label. This is required. All other labels
	 * are optional.
	 *
	 * @param primaryLabel     The primary label this node is identified by.
	 * @param additionalLabels Additional labels
	 * @return A new node representation
	 */
	public static Node node(String primaryLabel, String... additionalLabels) {

		return Node.create(primaryLabel, additionalLabels);
	}

	/**
	 * @return A node matching any node.
	 */
	public static Node anyNode() {
		return Node.create();
	}

	/**
	 * @return The {@code *} wildcard literal.
	 */
	public static Asterisk asterisk() {
		return Asterisk.INSTANCE;
	}

	/**
	 * @return A node matching any node with the symbolic the given {@code symbolicName}.
	 */
	public static Node anyNode(String symbolicName) {
		return Node.create().named(symbolicName);
	}

	/**
	 * Dereferences a property for a symbolic name, most likely pointing to a property container like a node or a relationship.
	 *
	 * @param containerName The symbolic name of a property container
	 * @param name          The name of the property to dereference
	 * @return A new property
	 */
	public static Property property(String containerName, String name) {
		return property(symbolicName(containerName), name);
	}

	/**
	 * Dereferences a property on a arbitrary expression.
	 *
	 * @param expression The expression that describes some sort of accessible map
	 * @param name       The name of the property to dereference
	 * @return A new property.
	 */
	public static Property property(Expression expression, String name) {
		return Property.create(expression, name);
	}

	/**
	 * Creates a new symbolic name.
	 *
	 * @param value The value of the symbolic name
	 * @return A new symoblic name
	 */
	public static SymbolicName symbolicName(String value) {
		return new SymbolicName(value);
	}

	/**
	 * Creates a new parameter placeholder. Existing $-signs will be removed.
	 *
	 * @param name The name of the parameter, must not be null
	 * @return The new parameter
	 */
	public static Parameter parameter(String name) {
		return Parameter.create(name);
	}

	/**
	 * Prepares an optional match statement.
	 *
	 * @param pattern The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 */
	public static ExposesMatch optionalMatch(PatternElement... pattern) {

		return Statement.builder().optionalMatch(pattern);
	}

	/**
	 * Starts building a statement based on a match clause. Use {@link Cypher#node(String, String...)} and related to
	 * retrieve a node or a relationship, which both are pattern elements.
	 *
	 * @param pattern The patterns to match
	 * @return An ongoing match that is used to specify an optional where and a required return clause
	 */
	public static OngoingMatchWithoutWhere match(PatternElement... pattern) {

		return Statement.builder().match(pattern);
	}

	/**
	 * Creates a new {@link SortItem} to be used as part of an {@link Order}.
	 *
	 * @param expression The expression by which things should be sorted
	 * @return A sort item, providing means to specify ascending or descending order
	 */
	public static SortItem sort(Expression expression) {

		return SortItem.create(expression, null);
	}

	/**
	 * Creates a map of expression from a list of key/value pairs.
	 *
	 * @param keysAndValues A list of key and values. Must be an even number, with alternating {@link String} and {@link Expression}
	 * @return A new map expression.
	 */
	public static MapExpression mapOf(Object... keysAndValues) {

		return MapExpression.create(keysAndValues);
	}

	/**
	 * Creates a new {@link NullLiteral} from the given {@code object}.
	 *
	 * @param object the object to represent.
	 * @return a new {@link NullLiteral}.
	 * @throws IllegalArgumentException when the object cannot be represented as a literal
	 */
	public static Literal literalOf(@Nullable Object object) {

		if (object == null) {
			return NullLiteral.INSTANCE;
		}
		if (object instanceof CharSequence) {
			return new StringLiteral((CharSequence) object);
		}
		if (object instanceof Number) {
			return new NumberLiteral((Number) object);
		}
		throw new IllegalArgumentException("Unsupported literal type: " + object.getClass());
	}

	/**
	 * @return The {@literal true} literal.
	 */
	public static Literal literalTrue() {
		return BooleanLiteral.TRUE;
	}

	/**
	 * @return The {@literal false} literal.
	 */
	public static Literal literalFalse() {
		return BooleanLiteral.FALSE;
	}

	/**
	 * Not to be instantiated.
	 */
	private Cypher() {
	}
}
