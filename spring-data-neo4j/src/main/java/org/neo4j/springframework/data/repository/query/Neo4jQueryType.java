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
package org.neo4j.springframework.data.repository.query;

import java.util.function.Function;

import org.springframework.data.repository.query.parser.PartTree;

/**
 * Describes the type of a query. The types are mutually exclusive.
 *
 * @author Michael J. Simons
 */
enum Neo4jQueryType {

	/**
	 * A query without projection.
	 */
	DEFAULT,
	/**
	 * Query with a count projection.
	 */
	COUNT,
	/**
	 * Query with an exists projection.
	 */
	EXISTS,
	/**
	 * Query to delete all matched results.
	 */
	DELETE;

	static Neo4jQueryType fromPartTree(PartTree partTree) {

		return getOrThrow(partTree.isCountProjection(), partTree.isExistsProjection(), partTree.isDelete());
	}

	static Neo4jQueryType fromDefinition(Query definition) {

		return getOrThrow(definition.count(), definition.exists(), definition.delete());
	}

	/**
	 * Gets the corresponding query type or throws an exception if the definition is not unique.
	 *
	 * @param countQuery True if you want a query with count projection.
	 * @param existsQuery True if you want a query with exists projection.
	 * @param deleteQuery True if you want a delete query.
	 * @return the query type
	 * @throws IllegalArgumentException in case more than one parameter is true.
	 */
	private static Neo4jQueryType getOrThrow(boolean countQuery, boolean existsQuery, boolean deleteQuery) {

		Neo4jQueryType queryType = DEFAULT;
		Function<Neo4jQueryType, IllegalArgumentException> exceptionSupplier = qt -> new IllegalArgumentException(
			"Query type already defined as " + qt);

		if (countQuery) {
			queryType = COUNT;
		}
		if (existsQuery) {
			if (queryType != DEFAULT) {
				throw exceptionSupplier.apply(queryType);
			}

			queryType = EXISTS;
		}

		if (deleteQuery) {
			if (queryType != DEFAULT) {
				throw exceptionSupplier.apply(queryType);
			}

			queryType = DELETE;
		}

		return queryType;
	}
}
