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
	 * @param pattern patterns to match
	 * @return An ongoing match
	 * @see Cypher#optionalMatch(PatternElement...)
	 */
	OngoingReadingWithoutWhere optionalMatch(PatternElement... pattern);

	/**
	 * @param pattern patterns to match
	 * @return An ongoing match
	 * @see Cypher#match(PatternElement...)
	 */
	OngoingReadingWithoutWhere match(PatternElement... pattern);

	/**
	 * @param pattern patterns to create
	 * @return An ongoing merge
	 * @see Cypher#create(PatternElement...)
	 */
	<T extends OngoingUpdate & ExposesSet> T create(PatternElement... pattern);

	/**
	 * @param pattern patterns to merge
	 * @return An ongoing merge
	 * @see Cypher#merge(PatternElement...)
	 */
	<T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern);

	OngoingUnwind unwind(Expression expression);

	OrderableOngoingReadingAndWith with(AliasedExpression... expressions);

	/**
	 * An ongoing update statement that can be used to chain more update statements or add a with or return clause.
	 * @since 1.0
	 */
	interface OngoingUpdate extends BuildableStatement, ExposesCreate, ExposesMerge, ExposesDelete, ExposesReturning, ExposesWith {
	}

	/**
	 * A match that exposes {@code returning} and {@code where} methods to add required information.
	 * While the where clause is optional, an returning clause needs to be specified before the
	 * statement can be build.
	 * @since 1.0
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
	 * @since 1.0
	 */
	interface OngoingReadingWithWhere extends OngoingReading, ExposesMatch, ExposesConditions<OngoingReadingWithWhere> {
	}

	/**
	 * @param <T> Type of ongoing query.
	 * @since 1.0
	 */
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
	 * @since 1.0
	 */
	interface OngoingReading extends ExposesReturning, ExposesWith, ExposesUpdatingClause, ExposesUnwind, ExposesCreate {
	}

	/**
	 * Builder part for unwinding.
	 * @since 1.0
	 */
	interface OngoingUnwind {

		OngoingReading as(String variable);
	}

	/**
	 * A match that knows what to return and which is ready to be build.
	 * @since 1.0
	 */
	interface OngoingReadingAndReturn
		extends TerminalExposesOrderBy, TerminalExposesSkip, TerminalExposesLimit, BuildableStatement {
	}

	/**
	 * A match that knows what to pipe to the next part of a multi part query.
	 * @since 1.0
	 */
	interface OrderableOngoingReadingAndWithWithoutWhere extends OrderableOngoingReadingAndWith {

		/**
		 * Adds a where clause to this match.
		 *
		 * @param condition The new condition
		 * @return A match restricted by a where clause with no return items yet.
		 */
		OrderableOngoingReadingAndWithWithWhere where(Condition condition);
	}

	/**
	 * @see OrderableOngoingReadingAndWith
	 * @see ExposesConditions
	 * @since 1.0
	 */
	interface OrderableOngoingReadingAndWithWithWhere
		extends OrderableOngoingReadingAndWith, ExposesConditions<OrderableOngoingReadingAndWithWithWhere> {
	}

	/**
	 * Represents a reading statement ending in a with clause, potentially already having an order and not exposing
	 * order methods.
	 *
	 * @since 1.0
	 */
	interface OngoingReadingAndWith
		extends OngoingReading, ExposesMatch, ExposesReturning, ExposesCreate, ExposesMerge {
	}

	/**
	 * @see OngoingReading
	 * @since 1.0
	 */
	interface OrderableOngoingReadingAndWith
		extends OngoingReading, ExposesMatch, ExposesOrderBy, ExposesSkip, ExposesLimit, ExposesReturning,
		ExposesCreate, ExposesMerge {
	}

	/**
	 * Combines the capabilities of skip, limit and adds additional expressions to the order-by items.
	 * @since 1.0
	 */
	interface OngoingMatchAndReturnWithOrder extends TerminalExposesSkip, TerminalExposesLimit, BuildableStatement {

		/**
		 * Adds another expression to the list of order items.
		 *
		 * @return A new order specifying step.
		 */
		TerminalOngoingOrderDefinition and(Expression expression);
	}

	/**
	 * An intermediate step while defining the order of a result set. This definitional will eventually return a
	 * buildable statement and thus is terminal.
	 *
	 * @since 1.0
	 */
	interface TerminalOngoingOrderDefinition extends TerminalExposesSkip, TerminalExposesLimit {

		/**
		 * Specifies descending order and jumps back to defining the match and return statement.
		 *
		 * @return The ongoing definition of a match
		 */
		<T extends TerminalExposesSkip & TerminalExposesLimit & OngoingMatchAndReturnWithOrder> T descending();

		/**
		 * Specifies ascending order and jumps back to defining the match and return statement.
		 *
		 * @return The ongoing definition of a match
		 */
		<T extends TerminalExposesSkip & TerminalExposesLimit & OngoingMatchAndReturnWithOrder> T ascending();
	}

	/**
	 * Combines the capabilities of skip, limit and adds additional expressions to the order-by items.
	 *
	 * @since 1.0
	 */
	interface OngoingReadingAndWithWithWhereAndOrder extends ExposesSkip, ExposesLimit,
		OngoingReadingAndWith {

		/**
		 * Adds another expression to the list of order items.
		 *
		 * @return A new order specifying step.
		 */
		OngoingOrderDefinition and(Expression expression);
	}

	/**
	 * An intermediate step while defining the order of a with clause.
	 *
	 * @since 1.0
	 */
	interface OngoingOrderDefinition extends ExposesSkip, ExposesLimit {

		/**
		 * Specifies descending order and jumps back to defining the match and return statement.
		 *
		 * @return The ongoing definition of a match
		 */
		<T extends ExposesSkip & ExposesLimit & OngoingReadingAndWithWithWhereAndOrder> T descending();

		/**
		 * Specifies ascending order and jumps back to defining the match and return statement.
		 *
		 * @return The ongoing definition of a match
		 */
		<T extends ExposesSkip & ExposesLimit & OngoingReadingAndWithWithWhereAndOrder> T ascending();
	}

	/**
	 * A statement that has all information required to be build and exposes a build method.
	 * @since 1.0
	 */
	interface BuildableStatement {

		/**
		 * @return The statement ready to be used, i.e. in a renderer.
		 */
		Statement build();
	}

	/**
	 * Return part of a statement.
	 * @since 1.0
	 */
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
	 *
	 * @since 1.0
	 */
	interface ExposesWith {

		default OrderableOngoingReadingAndWithWithoutWhere with(String... variables) {
			return with(createSymbolicNames(variables));
		}

		/**
		 * Create a match that returns one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OrderableOngoingReadingAndWithWithoutWhere with(Expression... expressions);

		default OrderableOngoingReadingAndWithWithoutWhere withDistinct(String... variables) {
			return withDistinct(createSymbolicNames(variables));
		}

		/**
		 * Create a match that returns the distinct set of one or more expressions.
		 *
		 * @param expressions The expressions to be returned. Must not be null and be at least one expression.
		 * @return A match that can be build now
		 */
		OrderableOngoingReadingAndWithWithoutWhere withDistinct(Expression... expressions);
	}

	/**
	 * A step that exposes several methods to specify ordering. This is a terminal operation just before a statement
	 * is buildable.
	 *
	 * @since 1.0
	 */
	interface TerminalExposesOrderBy {

		/**
		 * Order the result set by one or more {@link SortItem sort items}. Those can be retrieved for
		 * all expression with {@link Cypher#sort(Expression)} or directly from properties.
		 *
		 * @param sortItem One or more sort items
		 * @param <T>      The type of the step being returned
		 * @return A build step that still offers methods for defining skip and limit
		 */
		<T extends TerminalExposesSkip & TerminalExposesLimit & BuildableStatement> T orderBy(SortItem... sortItem);

		/**
		 * Order the result set by an expression.
		 *
		 * @param expression The expression to order by
		 * @return A step that allows for adding more expression or fine-tuning the sort direction of the last expression
		 */
		TerminalOngoingOrderDefinition orderBy(Expression expression);
	}

	/**
	 * A step that exposes the {@link #skip(Number)} method.
	 * @since 1.0
	 */
	interface TerminalExposesSkip {

		/**
		 * Adds a skip clause, skipping the given number of records.
		 *
		 * @param number How many records to skip. If this is null, then no records are skipped.
		 * @return A step that only allows the limit of records to be specified.
		 */
		<T extends TerminalExposesLimit & BuildableStatement> T skip(@Nullable Number number);

	}

	/**
	 * A step that exposes the {@link #limit(Number)} method.
	 * @since 1.0
	 */
	interface TerminalExposesLimit {

		/**
		 * Limits the number of returned records.
		 * @param number How many records to return. If this is null, all the records are returned.
		 * @return A buildable match statement.
		 */
		BuildableStatement limit(@Nullable Number number);
	}

	/**
	 * See {@link TerminalExposesOrderBy}, but on a with clause.
	 *
	 * @since 1.0
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
		<T extends ExposesSkip & ExposesLimit & OngoingReadingAndWith> T orderBy(SortItem... sortItem);

		/**
		 * Order the result set by an expression.
		 *
		 * @param expression The expression to order by
		 * @return A step that allows for adding more expression or fine-tuning the sort direction of the last expression
		 */
		OngoingOrderDefinition orderBy(Expression expression);
	}

	/**
	 * A step that exposes the {@link #skip(Number)} method.
	 *
	 * @since 1.0
	 */
	interface ExposesSkip {

		/**
		 * Adds a skip clause, skipping the given number of records.
		 *
		 * @param number How many records to skip. If this is null, then no records are skipped.
		 * @return A step that only allows the limit of records to be specified.
		 */
		<T extends ExposesLimit & OngoingReadingAndWith> T skip(@Nullable Number number);

	}

	/**
	 * A step that exposes the {@link #limit(Number)} method.
	 *
	 * @since 1.0
	 */
	interface ExposesLimit {

		/**
		 * Limits the number of returned records.
		 * @param number How many records to return. If this is null, all the records are returned.
		 * @return A buildable match statement.
		 */
		OngoingReadingAndWith limit(@Nullable Number number);
	}

	/**
	 * A step providing all the supported updating clauses (DELETE, SET)
	 * @since 1.0
	 */
	interface ExposesUpdatingClause extends ExposesDelete, ExposesMerge, ExposesSetAndRemove {
	}

	/**
	 * A step that exposes only the delete clause.
	 * @since 1.0
	 */
	interface ExposesDelete {

		default <T extends OngoingUpdate & BuildableStatement> T delete(String... variables) {
			return delete(createSymbolicNames(variables));
		}

		/**
		 * Creates a delete step with one or more expressions to be deleted.
		 *
		 * @param expressions The expressions to be deleted.
		 * @return A match with a delete clause that can be build now
		 */
		<T extends OngoingUpdate & BuildableStatement> T delete(Expression... expressions);

		default OrderableOngoingReadingAndWithWithoutWhere detachDelete(String... variables) {
			return detachDelete(createSymbolicNames(variables));
		}

		/**
		 * Starts building a delete step that will use {@code DETACH} to remove relationships.
		 *
		 * @param expressions The expressions to be deleted.
		 * @return A match with a delete clause that can be build now
		 */
		<T extends OngoingUpdate & BuildableStatement> T detachDelete(Expression... expressions);
	}

	/**
	 * Set part of a statement.
	 * @since 1.0
	 */
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
	 * @since 1.0
	 */
	interface ExposesSetAndRemove extends ExposesSet {

		<T extends OngoingMatchAndUpdate & BuildableStatement> T set(Node node, String... label);

		<T extends OngoingMatchAndUpdate & BuildableStatement> T remove(Node node, String... label);

		<T extends OngoingMatchAndUpdate & BuildableStatement> T remove(Property... properties);
	}

	/**
	 * A step exposing a {@link #match(PatternElement...)} method.
	 * @since 1.0
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

	/**
	 * A step exposing a {@link #create(PatternElement...)} method.
	 * @since 1.0
	 */
	interface ExposesCreate {

		<T extends OngoingUpdate & ExposesSet> T create(PatternElement... pattern);
	}

	/**
	 * A step exposing a {@link #merge(PatternElement...)} method.
	 * @since 1.0
	 */
	interface ExposesMerge {

		<T extends OngoingUpdate & ExposesSet> T merge(PatternElement... pattern);
	}

	/**
	 * A step exposing a {@link #unwind(Expression...)},{@link #unwind(Expression)}, {@link #unwind(String)} and method.
	 * @since 1.0
	 */
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
	 * @since 1.0
	 */
	interface OngoingMatchAndUpdate extends ExposesReturning, ExposesWith, ExposesUpdatingClause {
	}
}
