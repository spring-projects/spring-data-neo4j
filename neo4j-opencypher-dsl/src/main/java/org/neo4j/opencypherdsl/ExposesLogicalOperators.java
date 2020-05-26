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

/**
 * A step exposing logical operators {@code and} and {@code or} after a {@code where} clause.
 *
 * @author Michael J. Simons
 * @param <T> The type being returned after the new condition has been chained
 * @since 1.1
 */
public interface ExposesLogicalOperators<T> {

	/**
	 * Adds an additional condition to the existing conditions, connected by an {@literal and}.
	 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
	 * conditions used another logical operator.
	 *
	 * @param condition An additional condition
	 * @return The ongoing definition of a match
	 */
	T and(Condition condition);

	/**
	 * Adds an additional condition based on a path pattern to the existing conditions, connected by an {@literal and}.
	 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
	 * conditions used another logical operator.
	 *
	 * @param pathPattern An additional pattern to include in the conditions
	 * @return The ongoing definition of a match
	 */
	default T and(RelationshipPattern pathPattern) {
		return this.and(new RelationshipPatternCondition(pathPattern));
	}

	/**
	 * Adds an additional condition to the existing conditions, connected by an {@literal or}.
	 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
	 * conditions used another logical operator.
	 *
	 * @param condition An additional condition
	 * @return The ongoing definition of a match
	 */
	T or(Condition condition);

	/**
	 * Adds an additional condition based on a path pattern to the existing conditions, connected by an {@literal or}.
	 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
	 * conditions used another logical operator.
	 *
	 * @param pathPattern An additional pattern to include in the conditions
	 * @return The ongoing definition of a match
	 */
	default T or(RelationshipPattern pathPattern) {
		return this.or(new RelationshipPatternCondition(pathPattern));
	}
}
