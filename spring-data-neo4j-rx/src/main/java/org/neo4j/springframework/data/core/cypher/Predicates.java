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

import java.util.Collections;

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * Factory methods for creating predicates.
 *
 * @author Michael J. Simons
 * @soundtrack Mine & Fatoni - Alle Liebe nachträglich
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Predicates {

	/**
	 * Creates a function invocation for the {@code exists()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-exists">exists</a>.
	 *
	 * @param property The property to be passed to {@code exists()}
	 * @return A function call for {@code exists()} for one property
	 */
	public static FunctionInvocation exists(Property property) {

		Assert.notNull(property, "The property for exists() is required.");

		return new FunctionInvocation("exists", property);
	}

	/**
	 * Creates a function invocation for the {@code exists()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-exists">exists</a>.
	 *
	 * @param pattern The pattern to be passed to {@code exists()}
	 * @return A function call for {@code exists()} for one pattern
	 */
	public static FunctionInvocation exists(RelationshipPattern pattern) {

		Assert.notNull(pattern, "The pattern for exists() is required.");

		return new FunctionInvocation("exists", new Pattern(Collections.singletonList(pattern)));
	}
}
