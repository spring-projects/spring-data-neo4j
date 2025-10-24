/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.apiguardian.api.API;

/**
 * FalkorDB client interface that abstracts the connection to FalkorDB. Provides low-level
 * operations for executing Cypher queries.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface FalkorDBClient {

	/**
	 * Execute a Cypher query and return the result.
	 * @param query the Cypher query to execute
	 * @return a queryable result
	 */
	QueryResult query(String query);

	/**
	 * Execute a Cypher query with parameters and return the result.
	 * @param query the Cypher query to execute
	 * @param parameters the parameters to bind to the query
	 * @return a queryable result
	 */
	QueryResult query(String query, Map<String, Object> parameters);

	/**
	 * Execute a Cypher query and map the result using the provided mapper.
	 * @param <T> the type of the result
	 * @param query the Cypher query to execute
	 * @param resultMapper the mapper to convert the result
	 * @return the mapped result
	 */
	<T> T query(String query, Function<QueryResult, T> resultMapper);

	/**
	 * Execute a Cypher query with parameters and map the result using the provided
	 * mapper.
	 * @param <T> the type of the result
	 * @param query the Cypher query to execute
	 * @param parameters the parameters to bind to the query
	 * @param resultMapper the mapper to convert the result
	 * @return the mapped result
	 */
	<T> T query(String query, Map<String, Object> parameters, Function<QueryResult, T> resultMapper);

	/**
	 * Execute a Cypher query asynchronously.
	 * @param query the Cypher query to execute
	 * @return a CompletableFuture of the queryable result
	 */
	CompletableFuture<QueryResult> queryAsync(String query);

	/**
	 * Execute a Cypher query with parameters asynchronously.
	 * @param query the Cypher query to execute
	 * @param parameters the parameters to bind to the query
	 * @return a CompletableFuture of the queryable result
	 */
	CompletableFuture<QueryResult> queryAsync(String query, Map<String, Object> parameters);

	/**
	 * Interface representing a query result from FalkorDB.
	 */
	interface QueryResult {

		/**
		 * Returns the records in this result.
		 * @return the records
		 */
		Iterable<Record> records();

		/**
		 * Returns true if this result contains records.
		 * @return true if records are available
		 */
		boolean hasRecords();

		/**
		 * Returns the statistics for this query execution.
		 * @return the query statistics
		 */
		QueryStatistics statistics();

	}

	/**
	 * Interface representing a record in a query result.
	 */
	interface Record {

		/**
		 * Returns the value at the given index.
		 * @param index the index
		 * @return the value
		 */
		Object get(int index);

		/**
		 * Returns the value for the given key.
		 * @param key the key
		 * @return the value
		 */
		Object get(String key);

		/**
		 * Returns all keys in this record.
		 * @return the keys
		 */
		Iterable<String> keys();

		/**
		 * Returns the size of this record.
		 * @return the size
		 */
		int size();

		/**
		 * Returns all values in this record.
		 * @return the values
		 */
		Iterable<Object> values();

	}

	/**
	 * Interface representing query execution statistics.
	 */
	interface QueryStatistics {

		/**
		 * Returns the number of nodes created.
		 * @return nodes created
		 */
		int nodesCreated();

		/**
		 * Returns the number of nodes deleted.
		 * @return nodes deleted
		 */
		int nodesDeleted();

		/**
		 * Returns the number of relationships created.
		 * @return relationships created
		 */
		int relationshipsCreated();

		/**
		 * Returns the number of relationships deleted.
		 * @return relationships deleted
		 */
		int relationshipsDeleted();

		/**
		 * Returns the number of properties set.
		 * @return properties set
		 */
		int propertiesSet();

		/**
		 * Returns the number of labels added.
		 * @return labels added
		 */
		int labelsAdded();

		/**
		 * Returns the number of labels removed.
		 * @return labels removed
		 */
		int labelsRemoved();

	}

}
