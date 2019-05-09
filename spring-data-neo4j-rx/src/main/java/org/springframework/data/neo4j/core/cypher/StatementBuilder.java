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

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface StatementBuilder {

	/**
	 * See {@link Cypher#optional()}.
	 *
	 * @return
	 */
	ExposesMatch optional();

	/**
	 * See {@link Cypher#match(PatternElement...)}.
	 *
	 * @param pattern
	 * @return
	 */
	OngoingMatchWithoutWhere match(PatternElement... pattern);

	/**
	 * A match that exposes {@code returning} and {@code where} methods to add required information.
	 * While the where clause is optional, an returning clause needs to be specified before the
	 * statement can be build.
	 */
	interface OngoingMatchWithoutWhere extends OngoingMatch, ExposesMatch {

		/**
		 * Marks the next match as optional match.
		 *
		 * @return A step exposing a {@link ExposesMatch#match(PatternElement...)} method for adding patterns to match.
		 */
		ExposesMatch optional();

		/**
		 * Adds a where clause to this match.
		 *
		 * @param condition The new condition
		 * @return A match restricted by a where clause with no return items yet.
		 */
		OngoingMatchWithWhere where(Condition condition);
	}

	/**
	 * A match that has a non-empty {@code where}-part. THe returning clause is still open.
	 */
	interface OngoingMatchWithWhere extends OngoingMatch, ExposesMatch {

		ExposesMatch optional();

		/**
		 * Adds an additional condition to the existing conditions, connected by an {@literal and}.
		 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
		 * conditions used another logical operator.
		 *
		 * @param condition An additional condition
		 * @return The ongoing definition of a match
		 */
		OngoingMatchWithWhere and(Condition condition);

		/**
		 * Adds an additional condition to the existing conditions, connected by an {@literal or}.
		 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
		 * conditions used another logical operator.
		 *
		 * @param condition An additional condition
		 * @return The ongoing definition of a match
		 */
		OngoingMatchWithWhere or(Condition condition);
	}

	/**
	 * A match that exposes {@code returning} and for which it is not decided whether the optional
	 * where part has been used or note.
	 */
	interface OngoingMatch extends ExposesReturning, OngoingDetachDelete {

		/**
		 * Starts building a delete step that will use {@code DETACH} to remove relationships.
		 *
		 * @return An ongoing delete step that is used to specify things to be deleted.
		 */
		OngoingDetachDelete detach();
	}

	/**
	 * A match that knows what to return and which is ready to be build.
	 */
	interface OngoingMatchAndReturn extends ExposesOrderBy, ExposesSkip, ExposesLimit {
	}

	interface OngoingMatchAndReturnWithOrder extends ExposesSkip, ExposesLimit {

		/**
		 * Adds another expression to the list of order items.
		 *
		 * @return A new order specifying step.
		 */
		OngoingOrderDefinition and(Expression expression);
	}

	/**
	 * An intermediate step while defining the order of a resultset.
	 */
	interface OngoingOrderDefinition extends ExposesSkip, ExposesLimit {

		/**
		 * Specifies descending order and jumps back to defining the match and return statement.
		 *
		 * @return The ongoing definition of a match
		 */
		<T extends ExposesSkip & ExposesLimit & OngoingMatchAndReturnWithOrder> T descending();

		/**
		 * Specifies ascending order and jumps back to defining the match and return statement.
		 *
		 * @return The ongoing definition of a match
		 */
		<T extends ExposesSkip & ExposesLimit & OngoingMatchAndReturnWithOrder> T ascending();
	}

	/**
	 * A match that has all information required to be build and exposes a build method.
	 */
	interface BuildableMatch {

		/**
		 * @return The statement ready to be used, i.e. in a renderer.
		 */
		Statement build();
	}

	interface ExposesReturning {
		/**
		 * Create a match that returns one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OngoingMatchAndReturn returning(Expression... expressions);
	}

	/**
	 * A step that exposes several methods to speficy ordering.
	 */
	interface ExposesOrderBy extends BuildableMatch {

		/**
		 * Order the result set by one or more {@link SortItem sort items}. Those can be retrieved for
		 * all expression with {@link Cypher#sort(Expression)} or directly from properties.
		 *
		 * @param sortItem One or more sort items
		 * @param <T>      The type of the step being returned
		 * @return A build step that still offers methods for defining skip and limit
		 */
		<T extends ExposesSkip & ExposesLimit> T orderBy(SortItem... sortItem);

		/**
		 * Order the result set by an expression.
		 *
		 * @param expression The expression to order by
		 * @return A step that allows for adding more expression or finetuning the sort direction of the last expression
		 */
		OngoingOrderDefinition orderBy(Expression expression);
	}

	/**
	 * A step that exposes the {@link #skip(Number)} method.
	 */
	interface ExposesSkip extends BuildableMatch {

		/**
		 * Adds a skip clause, skipping the given number of records.
		 *
		 * @param number How many records to skip
		 * @return A step that only allows the limit of records to be specified
		 */
		ExposesLimit skip(Number number);

	}

	/**
	 * A step that exposes the {@link #limit(Number)} method.
	 */
	interface ExposesLimit extends BuildableMatch {

		/**
		 * Limits the number of returned records.
		 * @param number How many records to return
		 * @return A buildable match statement
		 */
		BuildableMatch limit(Number number);
	}

	/**
	 * A step that exposes only the delete clause.
	 */
	interface OngoingDetachDelete {
		/**
		 * Creates a delete step with one or more expressions to be deleted.
		 *
		 * @param expressions The expressions to be deleted.
		 * @return A match with a delete clause that can be build now
		 */
		OngoingMatchAndDelete delete(Expression... expressions);
	}

	/**
	 * A step exposing a {@link #match(PatternElement...)} method.
	 */
	interface ExposesMatch {

		/**
		 * Adds another match clause.
		 *
		 * @param pattern The patterns to match
		 * @return An ongoing match that is used to specify an optional where and a required return clause
		 */
		OngoingMatchWithoutWhere match(PatternElement... pattern);
	}

	/**
	 * A buildable step that will create a MATCH ... DELETE statement.
	 */
	interface OngoingMatchAndDelete extends BuildableMatch, ExposesReturning {
	}
}
