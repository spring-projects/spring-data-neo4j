/*
 * Copyright 2011-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Statement;

import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;

/**
 * {@link FluentFindOperation} allows creation and execution of Neo4j find operations in a
 * fluent API style.
 * <p>
 * The starting {@literal domainType} is used for mapping the query provided via
 * {@code by} into the Neo4j specific representation. By default, the originating
 * {@literal domainType} is also used for mapping back the result. However, it is possible
 * to define a different {@literal returnType} via {@code as} to mapping the result.
 *
 * @author Michael Simons
 * @since 6.1
 */
@API(status = API.Status.STABLE, since = "6.1")
public interface FluentFindOperation {

	/**
	 * Start creating a find operation for the given {@literal domainType}.
	 * @param domainType must not be {@literal null}.
	 * @param <T> the domain tyoe
	 * @return new instance of {@link ExecutableFind}.
	 * @throws IllegalArgumentException if domainType is {@literal null}.
	 */
	<T> ExecutableFind<T> find(Class<T> domainType);

	/**
	 * Trigger find execution by calling one of the terminating methods from a state where
	 * no query is yet defined.
	 *
	 * @param <T> returned type
	 */
	interface TerminatingFindWithoutQuery<T> {

		/**
		 * Get all matching elements.
		 * @return never {@literal null}.
		 */
		List<T> all();

	}

	/**
	 * Triggers find execution by calling one of the terminating methods.
	 *
	 * @param <T> returned type
	 */
	interface TerminatingFind<T> extends TerminatingFindWithoutQuery<T> {

		/**
		 * Get exactly zero or one result.
		 * @return {@link Optional#empty()} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more
		 * than one match found.
		 */
		default Optional<T> one() {
			return Optional.ofNullable(oneValue());
		}

		/**
		 * Get exactly zero or one result.
		 * @return {@literal null} if no match found.
		 * @throws org.springframework.dao.IncorrectResultSizeDataAccessException if more
		 * than one match found.
		 */
		@Nullable T oneValue();

	}

	/**
	 * Terminating operations invoking the actual query execution.
	 *
	 * @param <T> returned type
	 */
	interface FindWithQuery<T> extends TerminatingFindWithoutQuery<T> {

		/**
		 * Set the filter query to be used.
		 * @param query must not be {@literal null}.
		 * @param parameter an optional parameter map
		 * @return new instance of {@link TerminatingFind}.
		 * @throws IllegalArgumentException if query is {@literal null}.
		 */
		TerminatingFind<T> matching(String query, Map<String, Object> parameter);

		/**
		 * Creates an executable query based on fragments and parameters. Hardly useful
		 * outside framework-code and we actively discourage using this method.
		 * @param queryFragmentsAndParameters encapsulated query fragments and parameters
		 * as created by the repository abstraction
		 * @return new instance of {@link TerminatingFind}.
		 * @throws IllegalArgumentException if queryFragmentsAndParameters is
		 * {@literal null}.
		 */
		TerminatingFind<T> matching(QueryFragmentsAndParameters queryFragmentsAndParameters);

		/**
		 * Set the filter query to be used.
		 * @param query must not be {@literal null}.
		 * @return new instance of {@link TerminatingFind}.
		 * @throws IllegalArgumentException if query is {@literal null}.
		 */
		default TerminatingFind<T> matching(String query) {
			return matching(query, Collections.emptyMap());
		}

		/**
		 * Set the filter {@link Statement statement} to be used.
		 * @param statement must not be {@literal null}.
		 * @param parameter will be merged with parameters in the statement. Parameters in
		 * {@code parameter} have precedence
		 * @return new instance of {@link TerminatingFind}.
		 * @throws IllegalArgumentException if statement is {@literal null}.
		 */
		TerminatingFind<T> matching(Statement statement, Map<String, Object> parameter);

		/**
		 * Set the filter {@link Statement statement} to be used.
		 * @param statement must not be {@literal null}.
		 * @return new instance of {@link TerminatingFind}.
		 * @throws IllegalArgumentException if criteria is {@literal null}.
		 */
		default TerminatingFind<T> matching(Statement statement) {
			return matching(statement, Collections.emptyMap());
		}

	}

	/**
	 * Result type override (Optional).
	 *
	 * @param <T> returned type
	 */
	interface FindWithProjection<T> extends FindWithQuery<T> {

		/**
		 * Define the target type fields should be mapped to. <br />
		 * Skip this step if you are anyway only interested in the original domain type.
		 * @param resultType must not be {@literal null}.
		 * @param <R> result type.
		 * @return new instance of {@link FindWithProjection}.
		 * @throws IllegalArgumentException if resultType is {@literal null}.
		 */
		<R> FindWithQuery<R> as(Class<R> resultType);

	}

	/**
	 * Entry point for creating executable find operations.
	 *
	 * @param <T> returned type
	 */
	interface ExecutableFind<T> extends FindWithProjection<T> {

	}

}
