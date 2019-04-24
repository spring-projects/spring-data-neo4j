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
 * @author Michael J. Simons
 * @since 1.0
 */
interface StatementBuilder {

	/**
	 * See {@link Cypher#match(PatternElement...)}.
	 *
	 * @param pattern
	 * @return
	 */
	OngoingMatch match(PatternElement... pattern);

	/**
	 * A match that knows what to return and which is ready to be build.
	 */
	interface OngoingMatchAndReturn extends BuildableMatch {
	}

	/**
	 * A match that exposes {@code returning} and {@code where} methods to add required information.
	 */
	interface OngoingMatch {

		/**
		 * Create a match that returns one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OngoingMatchAndReturn returning(Expression... expressions);

		/**
		 * Creates a match that returns one ore more nodes. If the nodes have a symbolic name, that symbolic name is
		 * used in the return clause, otherwise the whole node pattern.
		 *
		 * @param nodes The nodes to be returned. Must not be null and be at least one node.
		 * @return A match that can be build now
		 */
		OngoingMatchAndReturn returning(Node... nodes);

		/**
		 * Adds a where clause to this match.
		 *
		 * @param condition The new condition
		 * @return A match restricted by a where clause with no return items yet.
		 */
		OngoingMatch where(Condition condition);
	}

	/**
	 * A match that has all information required to be build.
	 */
	interface BuildableMatch {

		/**
		 * @return The statement ready to be used, i.e. in a renderer.
		 */
		Statement build();
	}
}
