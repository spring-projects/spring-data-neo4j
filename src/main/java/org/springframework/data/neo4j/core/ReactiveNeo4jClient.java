/*
 * Copyright 2011-2021 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.reactive.RxQueryRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.neo4j.core.Neo4jClient.BindSpec;
import org.springframework.lang.Nullable;

/**
 * Reactive Neo4j client. The main difference to the {@link Neo4jClient imperative Neo4j client} is the fact that all
 * operations will only be executed once something subscribes to the reactive sequence defined.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface ReactiveNeo4jClient {

	LogAccessor cypherLog = new LogAccessor(LogFactory.getLog("org.springframework.data.neo4j.cypher"));

	static ReactiveNeo4jClient create(Driver driver) {

		return new DefaultReactiveNeo4jClient(driver, null);
	}

	static ReactiveNeo4jClient create(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

		return new DefaultReactiveNeo4jClient(driver, databaseSelectionProvider);
	}

	/**
	 * @return A managed query runner
	 * @see #getQueryRunner(Mono)
	 * @since 6.2
	 */
	default Mono<RxQueryRunner> getQueryRunner() {
		return getQueryRunner(Mono.just(DatabaseSelection.undecided()));
	}

	/**
	 * Retrieves a query runner that will participate in ongoing Spring transactions (either in declarative
	 * (implicit via {@code @Transactional}) or in programmatically (explicit via transaction template) ones).
	 * This runner can be used with the Cypher-DSL for example.
	 * If the client cannot retrieve an ongoing Spring transaction, this runner will use auto-commit semantics.
	 *
	 * @param databaseSelection The target database.
	 * @return A managed query runner
	 * @since 6.2
	 */
	Mono<RxQueryRunner> getQueryRunner(Mono<DatabaseSelection> databaseSelection);

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether it's a match, merge, create or
	 * removal of things.
	 *
	 * @param cypher The cypher code that shall be executed
	 * @return A new CypherSpec
	 */
	RunnableSpec query(String cypher);

	/**
	 * Entrypoint for creating a new Cypher query based on a supplier. Doesn't matter at this point whether it's a match,
	 * merge, create or removal of things. The supplier can be an arbitrary Supplier that may provide a DSL for generating
	 * the Cypher statement.
	 *
	 * @param cypherSupplier A supplier of arbitrary Cypher code
	 * @return A runnable query specification.
	 */
	RunnableSpec query(Supplier<String> cypherSupplier);

	/**
	 * Delegates interaction with the default database to the given callback.
	 *
	 * @param callback A function receiving a reactive statement runner for database interaction that can optionally
	 *          return a publisher with none or exactly one element
	 * @param <T> The type of the result being produced
	 * @return A single publisher containing none or exactly one element that will be produced by the callback
	 */
	<T> OngoingDelegation<T> delegateTo(Function<RxQueryRunner, Mono<T>> callback);

	/**
	 * Returns the assigned database selection provider.
	 *
	 * @return The database selection provider - can be null
	 */
	@Nullable
	ReactiveDatabaseSelectionProvider getDatabaseSelectionProvider();

	/**
	 * @param <T> The resulting type of this mapping
	 * @since 6.0
	 */
	interface MappingSpec<T> extends RecordFetchSpec<T> {

		/**
		 * The mapping function is responsible to turn one record into one domain object. It will receive the record itself
		 * and in addition, the type system that the Neo4j Java-Driver used while executing the query.
		 *
		 * @param mappingFunction The mapping function used to create new domain objects
		 * @return A specification how to fetch one or more records.
		 */
		RecordFetchSpec<T> mappedBy(BiFunction<TypeSystem, Record, T> mappingFunction);
	}

	/**
	 * @param <T> The type to which the fetched records are eventually mapped
	 * @since 6.0
	 */
	interface RecordFetchSpec<T> {

		/**
		 * Fetches exactly one record and throws an exception if there are more entries.
		 *
		 * @return The one and only record.
		 */
		Mono<T> one();

		/**
		 * Fetches only the first record. Returns an empty holder if there are no records.
		 *
		 * @return The first record if any.
		 */
		Mono<T> first();

		/**
		 * Fetches all records.
		 *
		 * @return All records.
		 */
		Flux<T> all();
	}

	/**
	 * Contract for a runnable query that can be either run returning its result, run without results or be
	 * parameterized.
	 *
	 * @since 6.0
	 */
	interface RunnableSpec extends RunnableSpecTightToDatabase {

		/**
		 * Pins the previously defined query to a specific database.
		 *
		 * @param targetDatabase selected database to use. A {@literal null} value indicates the default database.
		 * @return A runnable query specification that is now tight to a given database.
		 */
		RunnableSpecTightToDatabase in(@Nullable String targetDatabase);
	}

	/**
	 * Contract for a runnable query inside a dedicated database.
	 *
	 * @since 6.0
	 */
	interface RunnableSpecTightToDatabase extends BindSpec<RunnableSpecTightToDatabase> {

		/**
		 * Create a mapping for each record return to a specific type.
		 *
		 * @param targetClass The class each record should be mapped to
		 * @param <T> The type of the class
		 * @return A mapping spec that allows specifying a mapping function
		 */
		<T> MappingSpec<T> fetchAs(Class<T> targetClass);

		/**
		 * Fetch all records mapped into generic maps
		 *
		 * @return A fetch specification that maps into generic maps
		 */
		RecordFetchSpec<Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results. It returns the drivers result summary, including various counters and
		 * other statistics.
		 *
		 * @return A mono containing the native summary of the query.
		 */
		Mono<ResultSummary> run();
	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> The type of the returned value.
	 * @since 6.0
	 */
	interface OngoingDelegation<T> extends RunnableDelegation<T> {

		/**
		 * Runs the delegation in the given target database.
		 *
		 * @param targetDatabase selected database to use. A {@literal null} value indicates the default database.
		 * @return An ongoing delegation
		 */
		RunnableDelegation<T> in(@Nullable String targetDatabase);
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
		 *
		 * @return The optional result of the callback that has been executed with the given database.
		 */
		Mono<T> run();
	}
}
