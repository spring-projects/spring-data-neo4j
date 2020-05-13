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
import static org.neo4j.opencypherdsl.Expressions.*;

import org.apiguardian.api.API;

/**
 * Return part of a statement.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public interface ExposesReturning {

	default StatementBuilder.OngoingReadingAndReturn returning(String... variables) {
		return returning(createSymbolicNames(variables));
	}

	default StatementBuilder.OngoingReadingAndReturn returning(Named... variables) {
		return returning(createSymbolicNames(variables));
	}

	/**
	 * Create a match that returns one or more expressions.
	 *
	 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
	 * @return A match that can be build now
	 */
	StatementBuilder.OngoingReadingAndReturn returning(Expression... expressions);

	default StatementBuilder.OngoingReadingAndReturn returningDistinct(String... variables) {
		return returningDistinct(createSymbolicNames(variables));
	}

	default StatementBuilder.OngoingReadingAndReturn returningDistinct(Named... variables) {
		return returningDistinct(createSymbolicNames(variables));
	}

	/**
	 * Create a match that returns the distinct set of one or more expressions.
	 *
	 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
	 * @return A match that can be build now
	 */
	StatementBuilder.OngoingReadingAndReturn returningDistinct(Expression... expressions);
}
