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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.StatementRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * Definition of a modern Neo4j client.
 *
 * TODO Create examples how to use the callbacks etc. with Springs TransactionTemplate to deal with rollbacks etc.
 * TODO database selection
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Neo4jClient {

	Logger cypherLog = LoggerFactory.getLogger("org.springframework.data.neo4j.cypher");

	static Neo4jClient create(Driver driver) {

		return new DefaultNeo4jClient(driver);
	}

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
	 * @return
	 */
	RunnableSpec query(Supplier<String> cypherSupplier);

	/**
	 * Delegates interaction with the default database to the given callback.
	 *
	 * @param callback A function receiving a statement runner for database interaction that can optionally return a result.
	 * @param <T>      The type of the result being produced
	 * @return A single result object or an empty optional if the callback didn't produce a result
	 */
	<T> OngoingDelegation<T> delegateTo(Function<StatementRunner, Optional<T>> callback);

	/**
	 * Takes a prepared query, containing all the information about the cypher template to be used, needed parameters and
	 * an optional mapping function, and turns it into an executable query.
	 *
	 * @param preparedQuery
	 * @param <T>           The type of the objects returned by this query.
	 * @return
	 */
	<T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery);

	/**
	 * Contract for a runnable query that can be either run returning it's result, run without results or be parameterized.
	 */
	interface RunnableSpec extends RunnableSpecTightToDatabase {

		/**
		 * Pins the previously defined query to a specific database.
		 *
		 * @param targetDatabase
		 * @return
		 */
		RunnableSpecTightToDatabase in(String targetDatabase);
	}

	/**
	 * Contract for a runnable query inside a dedicated database.
	 */
	interface RunnableSpecTightToDatabase extends BindSpec<RunnableSpecTightToDatabase> {

		/**
		 * Create a mapping for each record return to a specific type.
		 *
		 * @param targetClass The class each record should be mapped to
		 * @param <T>         The type of the class
		 * @return A mapping spec that allows specifying a mapping function
		 */
		<T> MappingSpec<Optional<T>, Collection<T>, T> fetchAs(Class<T> targetClass);

		/**
		 * Fetch all records mapped into generic maps
		 *
		 * @return A fetch specification that maps into generic maps
		 */
		RecordFetchSpec<Optional<Map<String, Object>>, Collection<Map<String, Object>>, Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results
		 *
		 * @return
		 */
		ResultSummary run();
	}

	/**
	 * Contract for binding parameters to a query.
	 *
	 * @param <S> This {@link BindSpec specs} own type
	 */
	interface BindSpec<S extends BindSpec<S>> {

		/**
		 * @param value The value to bind to a query
		 * @return An ongoing bind spec for specifying the name that {@code value} should be bound to or a binder function
		 */
		<T> OngoingBindSpec<T, S> bind(@Nullable T value);

		S bindAll(Map<String, Object> parameters);
	}

	/**
	 * Ongoing bind specification.
	 *
	 * @param <S> This {@link OngoingBindSpec specs} own type
	 */
	interface OngoingBindSpec<T, S extends BindSpec<S>> {

		/**
		 * Bind one convertible object to the given name.
		 *
		 * @param name The named parameter to bind the value to
		 * @return
		 */
		S to(String name);

		/**
		 * Use a binder function for the previously defined value.
		 *
		 * @param binder The binder function to create a map of parameters from the given value
		 * @return
		 */
		S with(Function<T, Map<String, Object>> binder);
	}

	/**
	 * @param <S> The type of the class holding zero or one result element
	 * @param <M> The type of the class holding zero or more result elements
	 * @param <T> The resulting type of this mapping
	 */
	interface MappingSpec<S, M, T> extends RecordFetchSpec<S, M, T> {

		/**
		 * The mapping function is responsible to turn one record into one domain object. It will receive the record
		 * itself and in addition, the type system that the Neo4j Java-Driver used while executing the query.
		 *
		 * @param mappingFunction The mapping function used to create new domain objects
		 * @return A specification how to fetch one or more records
		 */
		RecordFetchSpec<S, M, T> mappedBy(BiFunction<TypeSystem, Record, T> mappingFunction);
	}

	/**
	 * @param <S> The type of the class holding zero or one result element
	 * @param <M> The type of the class holding zero or more result elements
	 * @param <T> The type to which the fetched records are eventually mapped
	 */
	interface RecordFetchSpec<S, M, T> {

		/**
		 * Fetches exactly one record and throws an exception if there are more entries.
		 *
		 * @return The one and only record.
		 */
		S one();

		/**
		 * Fetches only the first record. Returns an empty holder if there are no records.
		 *
		 * @return The first record if any.
		 */
		S first();

		/**
		 * Fetches all records.
		 *
		 * @return All records.
		 */
		M all();
	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> The type of the returned value.
	 */
	interface OngoingDelegation<T> extends RunnableDelegation<T> {

		/**
		 * Runs the delegation in the given target database.
		 *
		 * @param targetDatabase
		 * @return An ongoing delegation
		 */
		RunnableDelegation<T> in(String targetDatabase);
	}

	/**
	 * A runnable delegation.
	 *
	 * @param <T>
	 */
	interface RunnableDelegation<T> {

		/**
		 * Runs the stored callback.
		 *
		 * @return
		 */
		Optional<T> run();
	}

	/**
	 * An interface for controlling query execution.
	 *
	 * @param <T>
	 */
	interface ExecutableQuery<T> {

		List<T> getResults();

		Optional<T> getSingleResult();

		T getRequiredSingleResult();
	}
}
