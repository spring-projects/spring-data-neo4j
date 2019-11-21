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
package org.neo4j.springframework.data.core;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.LogFactory;
import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.log.LogAccessor;
import org.springframework.lang.Nullable;

/**
 * Definition of a modern Neo4j client.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface Neo4jClient {

	// TODO Create examples how to use the callbacks etc. with Springs TransactionTemplate to deal with rollbacks etc.

	LogAccessor cypherLog = new LogAccessor(LogFactory.getLog("org.neo4j.springframework.data.cypher"));

	static Neo4jClient create(Driver driver) {

		return new DefaultNeo4jClient(driver);
	}

	/**
	 * Entrypoint for creating a new Cypher query. Doesn't matter at this point whether it's a match, merge, create or
	 * removal of things.
	 *
	 * @param cypher The cypher code that shall be executed
	 * @return A runnable query specification.
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
	 * @param callback A function receiving a statement runner for database interaction that can optionally return a result.
	 * @param <T>      The type of the result being produced
	 * @return A single result object or an empty optional if the callback didn't produce a result
	 */
	<T> OngoingDelegation<T> delegateTo(Function<QueryRunner, Optional<T>> callback);

	/**
	 * Contract for a runnable query that can be either run returning it's result, run without results or be parameterized.
	 * @since 1.0
	 */
	interface RunnableSpec extends RunnableSpecTightToDatabase {

		/**
		 * Pins the previously defined query to a specific database. A value of {@literal null} chooses the default database.
		 * The empty string {@literal ""} is not permitted.
		 *
		 * @param targetDatabase selected database to use
		 * @return A runnable query specification that is now tight to a given database.
		 */
		RunnableSpecTightToDatabase in(@Nullable String targetDatabase);
	}

	/**
	 * Contract for a runnable query inside a dedicated database.
	 * @since 1.0
	 */
	interface RunnableSpecTightToDatabase extends BindSpec<RunnableSpecTightToDatabase> {

		/**
		 * Create a mapping for each record return to a specific type.
		 *
		 * @param targetClass The class each record should be mapped to
		 * @param <T>         The type of the class
		 * @return A mapping spec that allows specifying a mapping function.
		 */
		<T> MappingSpec<T> fetchAs(Class<T> targetClass);

		/**
		 * Fetch all records mapped into generic maps
		 *
		 * @return A fetch specification that maps into generic maps.
		 */
		RecordFetchSpec<Map<String, Object>> fetch();

		/**
		 * Execute the query and discard the results. It returns the drivers result summary, including various counters
		 * and other statistics.
		 *
		 * @return The native summary of the query.
		 */
		ResultSummary run();
	}

	/**
	 * Contract for binding parameters to a query.
	 *
	 * @param <S> This {@link BindSpec specs} own type
	 * @since 1.0
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
	 * @param <T> Binding value type
	 * @since 1.0
	 */
	interface OngoingBindSpec<T, S extends BindSpec<S>> {

		/**
		 * Bind one convertible object to the given name.
		 *
		 * @param name The named parameter to bind the value to
		 * @return The bind specification itself for binding more values or execution.
		 */
		S to(String name);

		/**
		 * Use a binder function for the previously defined value.
		 *
		 * @param binder The binder function to create a map of parameters from the given value
		 * @return The bind specification itself for binding more values or execution.
		 */
		S with(Function<T, Map<String, Object>> binder);
	}

	/**
	 * @param <T> The resulting type of this mapping
	 * @since 1.0
	 */
	interface MappingSpec<T> extends RecordFetchSpec<T> {

		/**
		 * The mapping function is responsible to turn one record into one domain object. It will receive the record
		 * itself and in addition, the type system that the Neo4j Java-Driver used while executing the query.
		 *
		 * @param mappingFunction The mapping function used to create new domain objects
		 * @return A specification how to fetch one or more records.
		 */
		RecordFetchSpec<T> mappedBy(BiFunction<TypeSystem, Record, T> mappingFunction);
	}

	/**
	 * @param <T> The type to which the fetched records are eventually mapped
	 * @since 1.0
	 */
	interface RecordFetchSpec<T> {

		/**
		 * Fetches exactly one record and throws an exception if there are more entries.
		 *
		 * @return The one and only record.
		 */
		Optional<T> one();

		/**
		 * Fetches only the first record. Returns an empty holder if there are no records.
		 *
		 * @return The first record if any.
		 */
		Optional<T> first();

		/**
		 * Fetches all records.
		 *
		 * @return All records.
		 */
		Collection<T> all();
	}

	/**
	 * A contract for an ongoing delegation in the selected database.
	 *
	 * @param <T> The type of the returned value.
	 * @since 1.0
	 */
	interface OngoingDelegation<T> extends RunnableDelegation<T> {

		/**
		 * Runs the delegation in the given target database.
		 *
		 * @param targetDatabase selected database to use
		 * @return An ongoing delegation
		 */
		RunnableDelegation<T> in(String targetDatabase);
	}

	/**
	 * A runnable delegation.
	 *
	 * @param <T> the type that gets returned
	 * @since 1.0
	 */
	interface RunnableDelegation<T> {

		/**
		 * Runs the stored callback.
		 *
		 * @return The optional result of the callback that has been executed with the given database.
		 */
		Optional<T> run();
	}

	/**
	 * This is a utility method to verify and sanitize a database name.
	 *
	 * @param databaseName The database name to verify and sanitize
	 * @return A possibly trimmed name of the database.
	 * @throws IllegalArgumentException when the database name is not allowed with the underlying driver.
	 */
	static String verifyDatabaseName(String databaseName) {

		String newTargetDatabase = databaseName == null ? null : databaseName.trim();
		if (newTargetDatabase != null && newTargetDatabase.isEmpty()) {
			throw new IllegalArgumentException(
				"Either use null to indicate the default database or a valid database name. The empty string is not permitted.");
		}
		return newTargetDatabase;
	}
}
