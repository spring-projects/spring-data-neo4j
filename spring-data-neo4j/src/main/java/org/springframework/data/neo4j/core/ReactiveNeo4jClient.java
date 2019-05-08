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
package org.springframework.data.neo4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxStatementRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.data.neo4j.core.Neo4jClient.BindSpec;
import org.springframework.data.neo4j.core.Neo4jClient.MappingSpec;
import org.springframework.data.neo4j.core.Neo4jClient.RecordFetchSpec;

/**
 * Reactive Neo4j client. The main difference to the {@link Neo4jClient imperative Neo4j client} is the fact that all
 * operations will only be executed once something subscribes to the reactive sequence defined.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface ReactiveNeo4jClient {

	static ReactiveNeo4jClient create(Driver driver) {

		return new DefaultReactiveNeo4jClient(driver);
	}

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether it's a match, merge, create or
	 * removal of things.
	 *
	 * @param cypher The cypher code that shall be executed
	 * @return A new CypherSpec
	 */
	ReactiveRunnableSpec newQuery(String cypher);

	/**
	 * Entrypoint for creating a new Cypher query based on a supplier. Doesn't matter at this point whether it's a match,
	 * merge, create or removal of things. The supplier can be an arbitrary Supplier that may provide a DSL for generating
	 * the Cypher statement.
	 *
	 * @param cypherSupplier A supplier of arbitrary Cypher code
	 * @return
	 */
	ReactiveRunnableSpec newQuery(Supplier<String> cypherSupplier);

	/**
	 * Delegates interaction with the default database to the given callback.
	 *
	 * @param callback A function receiving a reactive statement runner for database interaction that can optionally return a publisher with none or exactly one element
	 * @param <T>      The type of the result being produced
	 * @return A single publisher containing none or exactly one element that will be produced by the callback
	 */
	<T> OngoingReactiveDelegation<T> delegateTo(Function<RxStatementRunner, Mono<T>> callback);

	/**
	 * Contract for a runnable query that can be either run returning it's result, run without results or be parameterized.
	 */
	interface ReactiveRunnableSpec extends ReactiveRunnableSpecTightToDatabase {

		/**
		 * Pins the previously defined query to a specific database.
		 *
		 * @param targetDatabase
		 * @return
		 */
		ReactiveRunnableSpecTightToDatabase in(String targetDatabase);
	}

	/**
	 * Contract for a runnable query inside a dedicated database.
	 */
	interface ReactiveRunnableSpecTightToDatabase extends BindSpec<ReactiveRunnableSpecTightToDatabase> {

		/**
		 * Create a mapping for each record return to a specific type.
		 *
		 * @param targetClass The class each record should be mapped to
		 * @param <T>         The type of the class
		 * @return A mapping spec that allows specifying a mapping function
		 */
		<T> MappingSpec<Mono<T>, Flux<T>, T> fetchAs(Class<T> targetClass);

		/**
		 * Fetch all records mapped into generic maps
		 *
		 * @return A fetch specification that maps into generic maps
		 */
		RecordFetchSpec<Mono<Map<String, Object>>, Flux<Map<String, Object>>, Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results
		 *
		 * @return
		 */
		Mono<ResultSummary> run();
	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> The type of the returned value.
	 */
	interface OngoingReactiveDelegation<T> extends ReactiveRunnableDelegation<T> {

		/**
		 * Runs the delegation in the given target database.
		 *
		 * @param targetDatabase
		 * @return An ongoing delegation
		 */
		ReactiveRunnableDelegation<T> in(String targetDatabase);
	}

	/**
	 * A runnable delegation.
	 *
	 * @param <T>
	 */
	interface ReactiveRunnableDelegation<T> {

		/**
		 * Runs the stored callback.
		 *
		 * @return
		 */
		Mono<T> run();
	}
}
