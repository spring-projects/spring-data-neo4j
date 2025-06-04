/*
 * Copyright 2011-2025 the original author or authors.
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

import java.io.Serial;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Record;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.core.log.LogAccessor;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;

/**
 * Definition of a modern Neo4j client.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jClient {

	/**
	 * This is a public API introduced to turn the logging of the infamous warning back
	 * on. {@code The query used a deprecated function: `id`.}
	 */
	AtomicBoolean SUPPRESS_ID_DEPRECATIONS = new AtomicBoolean(true);

	/**
	 * All Cypher statements executed will be logged here.
	 */
	LogAccessor cypherLog = new LogAccessor(LogFactory.getLog("org.springframework.data.neo4j.cypher"));

	/**
	 * Some methods of the {@link Neo4jClient} will be logged here.
	 */
	LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jClient.class));

	static Neo4jClient create(Driver driver) {

		return with(driver).build();
	}

	static Neo4jClient create(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {

		return with(driver).withDatabaseSelectionProvider(databaseSelectionProvider).build();
	}

	static Builder with(Driver driver) {

		return new Builder(driver);
	}

	/**
	 * This is a utility method to verify and sanitize a database name.
	 * @param databaseName the database name to verify and sanitize
	 * @return a possibly trimmed name of the database
	 * @throws IllegalArgumentException when the database name is not allowed with the
	 * underlying driver.
	 */
	@Nullable static String verifyDatabaseName(@Nullable String databaseName) {

		String newTargetDatabase = (databaseName != null) ? databaseName.trim() : null;
		if (newTargetDatabase != null && newTargetDatabase.isEmpty()) {
			throw new IllegalDatabaseNameException(newTargetDatabase);
		}
		return newTargetDatabase;
	}

	/**
	 * Retrieves a query runner matching the plain Neo4j Java Driver api bound to Spring
	 * transactions.
	 * @return a managed query runner
	 * @since 6.2
	 * @see #getQueryRunner(DatabaseSelection, UserSelection)
	 */
	default QueryRunner getQueryRunner() {
		return getQueryRunner(DatabaseSelection.undecided());
	}

	/**
	 * Retrieves a query runner matching the plain Neo4j Java Driver api bound to Spring
	 * transactions configured to use a specific database.
	 * @param databaseSelection the database to use
	 * @return a managed query runner
	 * @since 6.2
	 * @see #getQueryRunner(DatabaseSelection, UserSelection)
	 */
	default QueryRunner getQueryRunner(DatabaseSelection databaseSelection) {
		return getQueryRunner(databaseSelection, UserSelection.connectedUser());
	}

	/**
	 * Retrieves a query runner that will participate in ongoing Spring transactions
	 * (either in declarative (implicit via {@code @Transactional}) or in programmatically
	 * (explicit via transaction template) ones). This runner can be used with the
	 * Cypher-DSL for example. If the client cannot retrieve an ongoing Spring
	 * transaction, this runner will use auto-commit semantics.
	 * @param databaseSelection the target database
	 * @param asUser as an impersonated user. Requires Neo4j 4.4 and Driver 4.4
	 * @return a managed query runner
	 * @since 6.2
	 */
	QueryRunner getQueryRunner(DatabaseSelection databaseSelection, UserSelection asUser);

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether
	 * it's a match, merge, create or removal of things.
	 * @param cypher the cypher code that shall be executed
	 * @return a runnable query specification
	 */
	UnboundRunnableSpec query(String cypher);

	/**
	 * Entrypoint for creating a new Cypher query based on a supplier. Doesn't matter at
	 * this point whether it's a match, merge, create or removal of things. The supplier
	 * can be an arbitrary Supplier that may provide a DSL for generating the Cypher
	 * statement.
	 * @param cypherSupplier a supplier of arbitrary Cypher code
	 * @return a runnable query specification
	 */
	UnboundRunnableSpec query(Supplier<String> cypherSupplier);

	/**
	 * Delegates interaction with the default database to the given callback.
	 * @param callback a function receiving a statement runner for database interaction
	 * that can optionally return a result
	 * @param <T> the type of the result being produced
	 * @return a single result object or an empty optional if the callback didn't produce
	 * a result
	 */
	<T> OngoingDelegation<T> delegateTo(Function<QueryRunner, Optional<T>> callback);

	/**
	 * Returns the assigned database selection provider.
	 * @return the database selection provider - can be null
	 */
	@Nullable DatabaseSelectionProvider getDatabaseSelectionProvider();

	/**
	 * Contract for a runnable query that can be either run returning its result, run
	 * without results or be parameterized.
	 *
	 * @since 6.0
	 */
	interface RunnableSpec extends BindSpec<RunnableSpec> {

		/**
		 * Create a mapping for each record return to a specific type.
		 * @param targetClass the class each record should be mapped to
		 * @param <T> the type of the class
		 * @return a mapping spec that allows specifying a mapping function
		 */
		<T> MappingSpec<T> fetchAs(Class<T> targetClass);

		/**
		 * Fetch all records mapped into generic maps.
		 * @return a fetch specification that maps into generic maps
		 */
		RecordFetchSpec<Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results. It returns the drivers result
		 * summary, including various counters and other statistics.
		 * @return the native summary of the query
		 */
		ResultSummary run();

	}

	/**
	 * Contract for a runnable query specification which still can be bound to a specific
	 * database and an impersonated user.
	 *
	 * @since 6.2
	 */
	interface UnboundRunnableSpec extends RunnableSpec {

		/**
		 * Pins the previously defined query to a specific database. A value of
		 * {@literal null} chooses the default database. The empty string {@literal ""} is
		 * not permitted.
		 * @param targetDatabase selected database to use. A {@literal null} value
		 * indicates the default database.
		 * @return a runnable query specification that is now bound to a given database
		 */
		RunnableSpecBoundToDatabase in(String targetDatabase);

		/**
		 * Pins the previously defined query to an impersonated user. A value of
		 * {@literal null} chooses the user owning the physical connection. The empty
		 * string {@literal ""} is not permitted.
		 * @param asUser the name of the user to impersonate. A {@literal null} value
		 * indicates the connected user
		 * @return a runnable query specification that is now bound to a given database.
		 */
		RunnableSpecBoundToUser asUser(String asUser);

	}

	/**
	 * Contract for a runnable query inside a dedicated database.
	 *
	 * @since 6.0
	 */
	interface RunnableSpecBoundToDatabase extends RunnableSpec {

		RunnableSpecBoundToDatabaseAndUser asUser(String aUser);

	}

	/**
	 * Contract for a runnable query bound to a user to be impersonated.
	 *
	 * @since 6.2
	 */
	interface RunnableSpecBoundToUser extends RunnableSpec {

		RunnableSpecBoundToDatabaseAndUser in(String aDatabase);

	}

	/**
	 * Combination of {@link RunnableSpecBoundToDatabase} and
	 * {@link RunnableSpecBoundToUser}, can't be bound any further.
	 *
	 * @since 6.2
	 */
	interface RunnableSpecBoundToDatabaseAndUser extends RunnableSpec {

	}

	/**
	 * Contract for binding parameters to a query.
	 *
	 * @param <S> this {@link BindSpec specs} own type
	 * @since 6.0
	 */
	interface BindSpec<S extends BindSpec<S>> {

		/**
		 * Starts binding a value to a parameter.
		 * @param value the value to bind to a query
		 * @return an ongoing bind spec for specifying the name that {@code value} should
		 * be bound to or a binder function
		 * @param <T> type of the value
		 */
		<T> OngoingBindSpec<T, S> bind(@Nullable T value);

		S bindAll(Map<String, Object> parameters);

	}

	/**
	 * Ongoing bind specification.
	 *
	 * @param <S> this {@link OngoingBindSpec specs} own type
	 * @param <T> binding value type
	 * @since 6.0
	 */
	interface OngoingBindSpec<T, S extends BindSpec<S>> {

		/**
		 * Bind one convertible object to the given name.
		 * @param name the named parameter to bind the value to
		 * @return the bind specification itself for binding more values or execution
		 */
		S to(String name);

		/**
		 * Use a binder function for the previously defined value.
		 * @param binder the binder function to create a map of parameters from the given
		 * value
		 * @return the bind specification itself for binding more values or execution
		 */
		S with(Function<T, Map<String, Object>> binder);

	}

	/**
	 * Step for defining the mapping.
	 *
	 * @param <T> the resulting type of this mapping
	 * @since 6.0
	 */
	interface MappingSpec<T> extends RecordFetchSpec<T> {

		/**
		 * The mapping function is responsible to turn one record into one domain object.
		 * It will receive the record itself and in addition, the type system that the
		 * Neo4j Java-Driver used while executing the query.
		 * @param mappingFunction the mapping function used to create new domain objects
		 * @return a specification how to fetch one or more records.
		 */
		RecordFetchSpec<T> mappedBy(BiFunction<TypeSystem, Record, T> mappingFunction);

	}

	/**
	 * Final step that triggers fetching.
	 *
	 * @param <T> the type to which the fetched records are eventually mapped
	 * @since 6.0
	 */
	interface RecordFetchSpec<T> {

		/**
		 * Fetches exactly one record and throws an exception if there are more entries.
		 * @return the one and only record
		 */
		Optional<T> one();

		/**
		 * Fetches only the first record. Returns an empty holder if there are no records.
		 * @return the first record if any
		 */
		Optional<T> first();

		/**
		 * Fetches all records.
		 * @return all records
		 */
		Collection<T> all();

	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> the type of the returned value
	 * @since 6.0
	 */
	interface OngoingDelegation<T> extends RunnableDelegation<T> {

		/**
		 * Runs the delegation in the given target database.
		 * @param targetDatabase selected database to use. A {@literal null} value
		 * indicates the default database.
		 * @return an ongoing delegation
		 */
		RunnableDelegation<T> in(String targetDatabase);

	}

	/**
	 * A runnable delegation.
	 *
	 * @param <T> the type that gets returned
	 * @since 6.0
	 */
	interface RunnableDelegation<T> {

		/**
		 * Runs the stored callback.
		 * @return the optional result of the callback that has been executed with the
		 * given database
		 */
		Optional<T> run();

	}

	/**
	 * A builder for {@link Neo4jClient Neo4j clients}.
	 */
	@API(status = API.Status.STABLE, since = "6.2")
	@SuppressWarnings("HiddenField")
	final class Builder {

		final Driver driver;

		@Nullable
		DatabaseSelectionProvider databaseSelectionProvider;

		@Nullable
		UserSelectionProvider userSelectionProvider;

		@Nullable
		Neo4jConversions neo4jConversions;

		@Nullable
		Neo4jBookmarkManager bookmarkManager;

		private Builder(Driver driver) {
			this.driver = driver;
		}

		/**
		 * Configures the database selection provider. Make sure to use the same instance
		 * as for a possible
		 * {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}.
		 * During runtime, it will be checked if a call is made for the same database when
		 * happening in a managed transaction.
		 * @param databaseSelectionProvider the database selection provider
		 * @return the builder
		 */
		public Builder withDatabaseSelectionProvider(@Nullable DatabaseSelectionProvider databaseSelectionProvider) {
			this.databaseSelectionProvider = databaseSelectionProvider;
			return this;
		}

		/**
		 * Configures a provider for impersonated users. Make sure to use the same
		 * instance as for a possible
		 * {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}.
		 * During runtime, it will be checked if a call is made for the same user when
		 * happening in a managed transaction.
		 * @param userSelectionProvider the provider for impersonated users
		 * @return the builder
		 */
		public Builder withUserSelectionProvider(@Nullable UserSelectionProvider userSelectionProvider) {
			this.userSelectionProvider = userSelectionProvider;
			return this;
		}

		/**
		 * Configures the set of {@link Neo4jConversions} to use.
		 * @param neo4jConversions the set of conversions to use, can be {@literal null},
		 * in this case the default set is used.
		 * @return the builder
		 * @since 6.3.3
		 */
		public Builder withNeo4jConversions(@Nullable Neo4jConversions neo4jConversions) {
			this.neo4jConversions = neo4jConversions;
			return this;
		}

		/**
		 * Configures the {@link Neo4jBookmarkManager} to use. This should be the same
		 * instance as provided for the
		 * {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}
		 * respectively the
		 * {@link org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager}.
		 * @param bookmarkManager the bookmark manager instance that is shared with the
		 * transaction manager
		 * @return the builder
		 * @since 7.1.2
		 */
		public Builder withNeo4jBookmarkManager(@Nullable Neo4jBookmarkManager bookmarkManager) {
			this.bookmarkManager = bookmarkManager;
			return this;
		}

		public Neo4jClient build() {
			return new DefaultNeo4jClient(this);
		}

	}

	/**
	 * Indicates an illegal database name and is not translated into a
	 * {@link org.springframework.dao.DataAccessException}.
	 *
	 * @since 6.1.5
	 */
	@API(status = API.Status.STABLE, since = "6.1.5")
	final class IllegalDatabaseNameException extends IllegalArgumentException {

		@Serial
		private static final long serialVersionUID = 3496326026855204643L;

		private final String illegalDatabaseName;

		private IllegalDatabaseNameException(String illegalDatabaseName) {
			super("Either use null to indicate the default database or a valid database name, the empty string is not permitted");
			this.illegalDatabaseName = illegalDatabaseName;
		}

		@SuppressWarnings("unused")
		public String getIllegalDatabaseName() {
			return this.illegalDatabaseName;
		}

	}

}
