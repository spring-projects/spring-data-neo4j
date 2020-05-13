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

import java.util.Arrays;

import org.apiguardian.api.API;

/**
 * Utility methods for dealing with expressions.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
final class Expressions {

	/**
	 * @param expression Possibly named with a non-empty symbolic name.
	 * @param <T> The type being returned
	 * @return The name of the expression if the expression is named or the expression itself.
	 */
	static <T> T nameOrExpression(T expression) {

		if (expression instanceof Named) {
			return ((Named) expression).getSymbolicName().map(v -> (T) v).orElse(expression);
		} else {
			return expression;
		}
	}

	static Expression[] createSymbolicNames(String[] variables) {
		return Arrays.stream(variables).map(SymbolicName::create).toArray(Expression[]::new);
	}

	static Expression[] createSymbolicNames(Named[] variables) {
		return Arrays.stream(variables).map(Named::getRequiredSymbolicName)
			.toArray(Expression[]::new);
	}

	private Expressions() {
	}
}
