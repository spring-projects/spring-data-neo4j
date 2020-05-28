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

import java.util.Collections;

import org.apiguardian.api.API;

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
	 * Creates a new condition based on a function invocation for the {@code exists()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-exists">exists</a>.
	 *
	 * @param property The property to be passed to {@code exists()}
	 * @return A function call for {@code exists()} for one property
	 */
	public static Condition exists(Property property) {

		Assert.notNull(property, "The property for exists() is required.");

		return new BooleanFunctionCondition(new FunctionInvocation("exists", property));
	}

	/**
	 * Creates a new condition based on a function invocation for the {@code exists()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-exists">exists</a>.
	 *
	 * @param pattern The pattern to be passed to {@code exists()}
	 * @return A function call for {@code exists()} for one pattern
	 */
	public static Condition exists(RelationshipPattern pattern) {

		Assert.notNull(pattern, "The pattern for exists() is required.");

		return new BooleanFunctionCondition(
			new FunctionInvocation("exists", new Pattern(Collections.singletonList(pattern))));
	}

	/**
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code all()} predicate function
	 * @see #all(SymbolicName)
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction all(String variable) {

		return all(SymbolicName.create(variable));
	}

	/**
	 * Starts building a new condition based on a function invocation for the {@code all()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-all">exists</a>.
	 *
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code all()} predicate function
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction all(SymbolicName variable) {

		return new Builder("all", variable);
	}

	/**
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code any()} predicate function
	 * @see #any(SymbolicName)
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction any(String variable) {

		return any(SymbolicName.create(variable));
	}

	/**
	 * Starts building a new condition based on a function invocation for the {@code any()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-any">exists</a>.
	 *
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code any()} predicate function
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction any(SymbolicName variable) {

		return new Builder("any", variable);
	}

	/**
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code none()} predicate function
	 * @see #none(SymbolicName)
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction none(String variable) {

		return none(SymbolicName.create(variable));
	}

	/**
	 * Starts building a new condition based on a function invocation for the {@code none()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-none">exists</a>.
	 *
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code none()} predicate function
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction none(SymbolicName variable) {

		return new Builder("none", variable);
	}

	/**
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code single()} predicate function
	 * @see #single(SymbolicName)
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction single(String variable) {

		return single(SymbolicName.create(variable));
	}

	/**
	 * Starts building a new condition based on a function invocation for the {@code single()} function.
	 * See <a href="https://neo4j.com/docs/cypher-manual/current/functions/predicate/#functions-single">exists</a>.
	 *
	 * @param variable The variable referring to elements of a list
	 * @return A builder for the {@code single()} predicate function
	 * @since 1.1
	 */
	public static OngoingListBasedPredicateFunction single(SymbolicName variable) {

		return new Builder("single", variable);
	}

	/**
	 * Allows to define the source of the list predicate.
	 */
	public interface OngoingListBasedPredicateFunction {

		/**
		 * @param list A list
		 * @return A builder to specify the where condition for the list based predicate
		 */
		OngoingListBasedPredicateFunctionWithList in(Expression list);
	}

	/**
	 * Allows to specify the where condition for the list based predicate.
	 */
	public interface OngoingListBasedPredicateFunctionWithList {

		/**
		 * @param condition The condition for the list based predicate.
		 * @return The final list based predicate function
		 */
		Condition where(Condition condition);
	}

	private static class Builder implements OngoingListBasedPredicateFunction,
		OngoingListBasedPredicateFunctionWithList {

		private final String predicate;
		private final SymbolicName name;
		private Expression listExpression;

		Builder(String predicate, SymbolicName name) {

			Assert.notNull(predicate, "The predicate is required");
			Assert.notNull(name, "The name is required");
			this.predicate = predicate;
			this.name = name;
		}

		@Override
		public OngoingListBasedPredicateFunctionWithList in(Expression list) {

			Assert.notNull(list, "The list expression is required");
			this.listExpression = list;
			return this;
		}

		@Override
		public Condition where(Condition condition) {

			Assert.notNull(condition, "The condition is required");
			return new BooleanFunctionCondition(
				new FunctionInvocation(predicate, new ListPredicate(name, listExpression, new Where(condition))));
		}
	}

	private Predicates() {
	}
}
