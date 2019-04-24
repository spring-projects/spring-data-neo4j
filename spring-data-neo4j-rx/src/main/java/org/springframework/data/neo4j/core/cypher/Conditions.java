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

/**
 * Builder for various conditions. Used internally from properties and other expressions that should take part in conditions.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public final class Conditions {

	/**
	 * Creates a condition that matches if the right hand side is a regular expression that matches the the left hand side via
	 * {@code =~}.
	 *
	 * @param lhs The left hand side of the comparision
	 * @param rhs The right hand side of the comparions
	 * @return A "matches" comparision
	 */
	static Condition matches(Expression lhs, Expression rhs) {
		return Comparison.create(lhs, "=~", rhs);
	}

	/**
	 * Not to be instantiated.
	 */
	private Conditions() {
	}
}
