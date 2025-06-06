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

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.reactivestreams.ReactiveQueryRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.log.LogAccessor;
import org.springframework.data.neo4j.core.Neo4jClient.BindSpec;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;

/**
 * Reactive Neo4j client. The main difference to the {@link Neo4jClient imperative Neo4j
 * client} is the fact that all operations will only be executed once something subscribes
 * to the reactive sequence defined.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface ReactiveNeo4jClient {

	/**
	 * All Cypher statements executed will be logged here.
	 */
	LogAccessor cypherLog = new LogAccessor(LogFactory.getLog("org.springframework.data.neo4j.cypher"));

	/**
	 * Some methods of the {@link ReactiveNeo4jClient} will be logged here.
	 */
	LogAccessor log = new LogAccessor(LogFactory.getLog(ReactiveNeo4jClient.class));

	static ReactiveNeo4jClient create(Driver driver) {

		return with(driver).build();
	}

	static ReactiveNeo4jClient create(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

		return with(driver).withDatabaseSelectionProvider(databaseSelectionProvider).build();
	}

	static Builder with(Driver driver) {

		return new Builder(driver);
	}

	/**
	 * Retrieves a query runner matching the plain Neo4j Java Driver api bound to Spring *
	 * transactions.
	 * @return a managed query runner
	 * @since 6.2
	 * @see #getQueryRunner(Mono)
	 */
	default Mono<ReactiveQueryRunner> getQueryRunner() {
		return getQueryRunner(Mono.just(DatabaseSelection.undecided()));
	}

	/**
	 * Retrieves a query runner matching the plain Neo4j Java Driver api bound to Spring *
	 * transactions configured to use a specific database.
	 * @param databaseSelection the database to use
	 * @return a managed query runner
	 * @since 6.2
	 * @see #getQueryRunner(Mono, Mono)
	 */
	default Mono<ReactiveQueryRunner> getQueryRunner(Mono<DatabaseSelection> databaseSelection) {
		return getQueryRunner(databaseSelection, Mono.just(UserSelection.connectedUser()));
	}

	/**
	 * Retrieves a query runner that will participate in ongoing Spring transactions
	 * (either in declarative (implicit via {@code @Transactional}) or in programmatically
	 * (explicit via transaction template) ones). This runner can be used with the
	 * Cypher-DSL for example. If the client cannot retrieve an ongoing Spring
	 * transaction, this runner will use auto-commit semantics.
	 * @param databaseSelection the target database.
	 * @param userSelection the user selection
	 * @return a managed query runner
	 * @since 6.2
	 */
	Mono<ReactiveQueryRunner> getQueryRunner(Mono<DatabaseSelection> databaseSelection,
			Mono<UserSelection> userSelection);

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether
	 * it's a match, merge, create or removal of things.
	 * @param cypher the cypher code that shall be executed
	 * @return a new CypherSpec
	 */
	UnboundRunnableSpec query(String cypher);

	/**
	 * Entrypoint for creating a new Cypher query based on a supplier. Doesn't matter at
	 * this point whether it's a match, merge, create or removal of things. The supplier
	 * can be an arbitrary Supplier that may provide a DSL for generating the Cypher
	 * statement.
	 * @param cypherSupplier a supplier of arbitrary Cypher code
	 * @return a runnable query specification.
	 */
	UnboundRunnableSpec query(Supplier<String> cypherSupplier);

	/**
	 * Delegates interaction with the default database to the given callback.
	 * @param callback a function receiving a reactive statement runner for database
	 * interaction that can optionally return a publisher with none or exactly one element
	 * @param <T> the type of the result being produced
	 * @return a single publisher containing none or exactly one element that will be
	 * produced by the callback
	 */
	<T> OngoingDelegation<T> delegateTo(Function<ReactiveQueryRunner, Mono<T>> callback);

	/**
	 * Returns the assigned database selection provider.
	 * @return the database selection provider - can be null
	 */
	@Nullable ReactiveDatabaseSelectionProvider getDatabaseSelectionProvider();

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
		 * @return the one and only record.
		 */
		Mono<T> one();

		/**
		 * Fetches only the first record. Returns an empty holder if there are no records.
		 * @return the first record if any.
		 */
		Mono<T> first();

		/**
		 * Fetches all records.
		 * @return all records.
		 */
		Flux<T> all();

	}

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
		 * @return a mono containing the native summary of the query.
		 */
		Mono<ResultSummary> run();

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
		 * @return a runnable query specification that is now bound to a given database.
		 */
		RunnableSpecBoundToDatabase in(String targetDatabase);

		/**
		 * Pins the previously defined query to an impersonated user. A value of
		 * {@literal null} chooses the user owning the physical connection. The empty
		 * string {@literal ""} is not permitted.
		 * @param asUser the name of the user to impersonate. A {@literal null} value
		 * indicates the connected user.
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
	 * Combination of {@link Neo4jClient.RunnableSpecBoundToDatabase} and
	 * {@link Neo4jClient.RunnableSpecBoundToUser}, can't be bound any further.
	 *
	 * @since 6.2
	 */
	interface RunnableSpecBoundToDatabaseAndUser extends RunnableSpec {

	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> the type of the returned value.
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
	 * @param <T> the type that gets returned by the query
	 * @since 6.0
	 */
	interface RunnableDelegation<T> {

		/**
		 * Runs the stored callback.
		 * @return the optional result of the callback that has been executed with the
		 * given database.
		 */
		Mono<T> run();

	}

	/**
	 * A builder for {@link ReactiveNeo4jClient reactive Neo4j clients}.
	 */
	@API(status = API.Status.STABLE, since = "6.2")
	@SuppressWarnings("HiddenField")
	final class Builder {

		final Driver driver;

		@Nullable
		ReactiveDatabaseSelectionProvider databaseSelectionProvider;

		@Nullable
		ReactiveUserSelectionProvider impersonatedUserProvider;

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
		public Builder withDatabaseSelectionProvider(
				@Nullable ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
			this.databaseSelectionProvider = databaseSelectionProvider;
			return this;
		}

		/**
		 * Configures a provider for impersonated users. Make sure to use the same
		 * instance as for a possible
		 * {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}.
		 * During runtime, it will be checked if a call is made for the same user when
		 * happening in a managed transaction.
		 * @param impersonatedUserProvider the provider for impersonated users
		 * @return the builder
		 */
		public Builder withUserSelectionProvider(@Nullable ReactiveUserSelectionProvider impersonatedUserProvider) {
			this.impersonatedUserProvider = impersonatedUserProvider;
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
		 * @param bookmarkManager the Neo4jBookmarkManager instance that is shared with
		 * the transaction manager.
		 * @return the builder
		 * @since 7.1.2
		 */
		public Builder withNeo4jBookmarkManager(@Nullable Neo4jBookmarkManager bookmarkManager) {
			this.bookmarkManager = bookmarkManager;
			return this;
		}

		public ReactiveNeo4jClient build() {
			return new DefaultReactiveNeo4jClient(this);
		}

	}

}
