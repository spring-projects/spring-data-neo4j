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
package org.neo4j.springframework.data.core.cypher;

import static org.neo4j.springframework.data.core.cypher.Expressions.*;

import org.apiguardian.api.API;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface StatementBuilder {

	/**
	 * @see Cypher#optionalMatch(PatternElement...)
	 * @return
	 */
	OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern);

	/**
	 * @see Cypher#match(PatternElement...)
	 * @param pattern
	 * @return
	 */
	OngoingReadingWithoutWhere match(PatternElement... pattern);

	/**
	 * @see Cypher#create(PatternElement...)
	 * @param pattern
	 * @return
	 */
	<T extends OngoingUpdate & ExposesSet> T create(PatternElement... pattern);

	/**
	 * @see Cypher#merge(PatternElement...)
	 * @param pattern
	 * @return
	 */
	<T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern);

	OngoingUnwind unwind(Expression expression);

	OngoingReadingAndWith with(AliasedExpression... expressions);

	/**
	 * An ongoing update statement that can be used to chain more update statements or add a with or return clause.
	 */
	interface OngoingUpdate extends BuildableStatement, ExposesCreate, ExposesMerge, ExposesReturning, ExposesWith {
	}

	/**
	 * A match that exposes {@code returning} and {@code where} methods to add required information.
	 * While the where clause is optional, an returning clause needs to be specified before the
	 * statement can be build.
	 */
	interface OngoingReadingWithoutWhere extends OngoingReading, ExposesMatch, ExposesCreate, ExposesMerge {

		/**
		 * Adds a where clause to this match.
		 *
		 * @param condition The new condition
		 * @return A match restricted by a where clause with no return items yet.
		 */
		OngoingReadingWithWhere where(Condition condition);
	}

	/**
	 * A match that has a non-empty {@code where}-part. THe returning clause is still open.
	 */
	interface OngoingReadingWithWhere extends OngoingReading, ExposesMatch, ExposesConditions<OngoingReadingWithWhere> {
	}

	interface ExposesConditions<T> {
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
		 * Adds an additional condition to the existing conditions, connected by an {@literal or}.
		 * Existing conditions will be logically grouped by using {@code ()} in the statement if previous
		 * conditions used another logical operator.
		 *
		 * @param condition An additional condition
		 * @return The ongoing definition of a match
		 */
		T or(Condition condition);
	}

	/**
	 * A match that exposes {@code returning} and for which it is not decided whether the optional
	 * where part has been used or note.
	 */
	interface OngoingReading extends ExposesReturning, ExposesWith, ExposesUpdatingClause, ExposesUnwind {
	}

	interface OngoingUnwind {

		OngoingReading as(String variable);
	}

	/**
	 * A match that knows what to return and which is ready to be build.
	 */
	interface OngoingReadingAndReturn extends ExposesOrderBy, ExposesSkip, ExposesLimit, BuildableStatement {
	}

	/**
	 * A match that knows what to pipe to the next part of a multi part query.
	 */
	interface OngoingReadingAndWithWithoutWhere extends OngoingReadingAndWith {

		/**
		 * Adds a where clause to this match.
		 *
		 * @param condition The new condition
		 * @return A match restricted by a where clause with no return items yet.
		 */
		OngoingReadingAndWithWithWhere where(Condition condition);
	}

	interface OngoingReadingAndWithWithWhere
		extends OngoingReadingAndWith, ExposesConditions<OngoingReadingAndWithWithWhere> {
	}

	interface OngoingReadingAndWith
		extends OngoingReading, ExposesMatch, ExposesOrderBy, ExposesSkip, ExposesLimit, ExposesReturning,
		ExposesCreate, ExposesMerge {
	}

	interface OngoingMatchAndReturnWithOrder extends ExposesSkip, ExposesLimit, BuildableStatement {

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
	 * A statement that has all information required to be build and exposes a build method.
	 */
	interface BuildableStatement {

		/**
		 * @return The statement ready to be used, i.e. in a renderer.
		 */
		Statement build();
	}

	interface ExposesReturning {

		default OngoingReadingAndReturn returning(String... variables) {
			return returning(createSymbolicNames(variables));
		}

		/**
		 * Create a match that returns one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OngoingReadingAndReturn returning(Expression... expressions);

		default OngoingReadingAndReturn returningDistinct(String... variables) {
			return returningDistinct(createSymbolicNames(variables));
		}


		/**
		 * Create a match that returns the distinct set of one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OngoingReadingAndReturn returningDistinct(Expression... expressions);
	}

	/**
	 * A step that exposes the {@code WITH} clause.
	 */
	interface ExposesWith {

		default OngoingReadingAndWithWithoutWhere with(String... variables) {
			return with(createSymbolicNames(variables));
		}

		/**
		 * Create a match that returns one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OngoingReadingAndWithWithoutWhere with(Expression... expressions);

		default OngoingReadingAndWithWithoutWhere withDistinct(String... variables) {
			return withDistinct(createSymbolicNames(variables));
		}

		/**
		 * Create a match that returns the distinct set of one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OngoingReadingAndWithWithoutWhere withDistinct(Expression... expressions);
	}

	/**
	 * A step that exposes several methods to specify ordering.
	 */
	interface ExposesOrderBy {

		/**
		 * Order the result set by one or more {@link SortItem sort items}. Those can be retrieved for
		 * all expression with {@link Cypher#sort(Expression)} or directly from properties.
		 *
		 * @param sortItem One or more sort items
		 * @param <T>      The type of the step being returned
		 * @return A build step that still offers methods for defining skip and limit
		 */
		<T extends ExposesSkip & ExposesLimit & BuildableStatement> T orderBy(SortItem... sortItem);

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
	interface ExposesSkip {

		/**
		 * Adds a skip clause, skipping the given number of records.
		 *
		 * @param number How many records to skip. If this is null, then no records are skipped.
		 * @return A step that only allows the limit of records to be specified.
		 */
		<T extends ExposesLimit & BuildableStatement> T skip(@Nullable Number number);

	}

	/**
	 * A step that exposes the {@link #limit(Number)} method.
	 */
	interface ExposesLimit {

		/**
		 * Limits the number of returned records.
		 * @param number How many records to return. If this is null, all the records are returned.
		 * @return A buildable match statement.
		 */
		BuildableStatement limit(@Nullable Number number);
	}

	/**
	 * A step providing all the supported updating clauses (DELETE, SET)
	 */
	interface ExposesUpdatingClause extends ExposesDelete, ExposesMerge, ExposesSetAndRemove {
	}

	/**
	 * A step that exposes only the delete clause.
	 */
	interface ExposesDelete {

		default OngoingReadingAndWithWithoutWhere delete(String... variables) {
			return delete(createSymbolicNames(variables));
		}

		/**
		 * Creates a delete step with one or more expressions to be deleted.
		 *
		 * @param expressions The expressions to be deleted.
		 * @return A match with a delete clause that can be build now
		 */
		<T extends OngoingMatchAndUpdate & BuildableStatement> T delete(Expression... expressions);

		default OngoingReadingAndWithWithoutWhere detachDelete(String... variables) {
			return detachDelete(createSymbolicNames(variables));
		}

		/**
		 * Starts building a delete step that will use {@code DETACH} to remove relationships.
		 *
		 * @param expressions The expressions to be deleted.
		 * @return A match with a delete clause that can be build now
		 */
		<T extends OngoingMatchAndUpdate & BuildableStatement> T detachDelete(Expression... expressions);
	}

	interface ExposesSet {

		/**
		 * Adds a {@code SET} clause to the statement. The list of expressions must be even, each pair will be turned into
		 * SET operation.
		 *
		 * @param expressions The list of expressions to use in a set clause.
		 * @return An ongoing match and update
		 */
		<T extends OngoingMatchAndUpdate & BuildableStatement> T set(Expression... expressions);
	}

	/**
	 * A step that exposes the set clause.
	 */
	interface ExposesSetAndRemove extends ExposesSet {

		<T extends OngoingMatchAndUpdate & BuildableStatement> T set(Node node, String... label);

		<T extends OngoingMatchAndUpdate & BuildableStatement> T remove(Node node, String... label);

		<T extends OngoingMatchAndUpdate & BuildableStatement> T remove(Property... properties);
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
		OngoingReadingWithoutWhere match(PatternElement... pattern);

		/**
		 * Adds another optional match clause.
		 *
		 * @param pattern The patterns to match
		 * @return An ongoing match that is used to specify an optional where and a required return clause
		 */
		OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern);
	}

	interface ExposesCreate {

		<T extends OngoingUpdate & ExposesSet> T create(PatternElement... pattern);
	}

	interface ExposesMerge {

		<T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern);
	}

	interface ExposesUnwind {

		default OngoingUnwind unwind(Expression... expressions) {
			return unwind(Cypher.listOf(expressions));
		}

		default OngoingUnwind unwind(String variable) {
			return unwind(Cypher.name(variable));
		}

		OngoingUnwind unwind(Expression expression);
	}

	/**
	 * A buildable step that will create a MATCH ... DELETE statement.
	 */
	interface OngoingMatchAndUpdate extends ExposesReturning, ExposesWith, ExposesUpdatingClause {
	}
}
