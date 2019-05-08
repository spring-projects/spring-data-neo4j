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
	 * Prepares an optional match statement.
	 *
	 * @return An optional match without any patterns yet.
	 */
	public static ExposesMatch optional() {

		return Statement.builder().optional();
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
	 * Creates a new {@link StringLiteral} from a {@code content}.
	 *
	 * @param content the literal content.
	 * @return a new {@link StringLiteral}.
	 */
	public static StringLiteral literalOf(@Nullable CharSequence content) {
		return new StringLiteral(content);
	}

	/**
	 * Creates a new {@link NumberLiteral} from the given {@code number}.
	 *
	 * @param number the number.
	 * @return a new {@link NumberLiteral}.
	 */
	public static NumberLiteral literalOf(@Nullable Number number) {
		return new NumberLiteral(number);
	}

	/**
	 * Creates a new {@link ObjectLiteral} from the given {@code object}.
	 *
	 * @param object the object to represent.
	 * @return a new {@link ObjectLiteral}.
	 */
	public static Literal literalOf(@Nullable Object object) {
		if (object instanceof CharSequence) {
			return new StringLiteral((CharSequence) object);
		}
		if (object instanceof Number) {
			return new NumberLiteral((Number) object);
		}
		return new ObjectLiteral(object);
	}

	/**
	 * Not to be instantiated.
	 */
	private Cypher() {
	}
}
