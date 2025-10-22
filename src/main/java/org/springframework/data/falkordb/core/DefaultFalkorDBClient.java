/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import com.falkordb.Driver;
import com.falkordb.Graph;
import org.apiguardian.api.API;

/**
 * Default implementation of {@link FalkorDBClient} using the JFalkorDB Java client. This
 * implementation uses the RESP protocol to communicate with FalkorDB.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public class DefaultFalkorDBClient implements FalkorDBClient {

	private final Driver driver;

	private final String graphName;

	private final ExecutorService executorService;

	public DefaultFalkorDBClient(Driver driver, String graphName) {
		this.driver = driver;
		this.graphName = graphName;
		this.executorService = Executors.newCachedThreadPool(r -> {
			Thread t = new Thread(r, "falkordb-async-");
			t.setDaemon(true);
			return t;
		});
	}

	@Override
	public QueryResult query(String query) {
		return query(query, Collections.emptyMap());
	}

	@Override
	public QueryResult query(String query, Map<String, Object> parameters) {
		try {
			Graph graph = this.driver.graph(this.graphName);
			com.falkordb.ResultSet resultSet;

			if (parameters.isEmpty()) {
				resultSet = graph.query(query);
			}
			else {
				resultSet = graph.query(query, parameters);
			}

			return new FalkorDBQueryResult(resultSet);
		}
		catch (Exception ex) {
			throw new RuntimeException("Error executing query: " + query, ex);
		}
	}

	@Override
	public <T> T query(String query, Function<QueryResult, T> resultMapper) {
		return query(query, Collections.emptyMap(), resultMapper);
	}

	@Override
	public <T> T query(String query, Map<String, Object> parameters, Function<QueryResult, T> resultMapper) {
		QueryResult result = query(query, parameters);
		return resultMapper.apply(result);
	}

	@Override
	public CompletableFuture<QueryResult> queryAsync(String query) {
		return queryAsync(query, Collections.emptyMap());
	}

	@Override
	public CompletableFuture<QueryResult> queryAsync(String query, Map<String, Object> parameters) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return query(query, parameters);
			}
			catch (Exception ex) {
				throw new CompletionException("Async query failed: " + query, ex);
			}
		}, this.executorService);
	}

	/**
	 * Clean shutdown of the client.
	 */
	public void shutdown() {
		this.executorService.shutdown();
	}

	/**
	 * Implementation of {@link QueryResult} that wraps JFalkorDB's ResultSet.
	 */
	private static class FalkorDBQueryResult implements QueryResult {

		private final com.falkordb.ResultSet resultSet;

		private final List<Record> records;

		FalkorDBQueryResult(com.falkordb.ResultSet resultSet) {
			this.resultSet = resultSet;
			this.records = new ArrayList<>();

			// Convert JFalkorDB records to our Record interface
			for (com.falkordb.Record jfRecord : resultSet) {
				this.records.add(new FalkorDBRecord(jfRecord));
			}
		}

		@Override
		public Iterable<Record> records() {
			return this.records;
		}

		@Override
		public boolean hasRecords() {
			return !this.records.isEmpty();
		}

		@Override
		public QueryStatistics statistics() {
			return new FalkorDBQueryStatistics(this.resultSet);
		}

	}

	/**
	 * Implementation of {@link Record} that wraps JFalkorDB's Record.
	 */
	private static class FalkorDBRecord implements Record {

		private final com.falkordb.Record record;

		FalkorDBRecord(com.falkordb.Record record) {
			this.record = record;
		}

		@Override
		public Object get(int index) {
			return this.record.getValue(index);
		}

		@Override
		public Object get(String key) {
			return this.record.getValue(key);
		}

		@Override
		public Iterable<String> keys() {
			return this.record.keys();
		}

		@Override
		public int size() {
			return this.record.size();
		}

		@Override
		public Iterable<Object> values() {
			List<Object> values = new ArrayList<>();
			for (int i = 0; i < this.record.size(); i++) {
				values.add(this.record.getValue(i));
			}
			return values;
		}

	}

	/**
	 * Implementation of {@link QueryStatistics} that wraps JFalkorDB's statistics.
	 */
	private static class FalkorDBQueryStatistics implements QueryStatistics {

		private final com.falkordb.ResultSet resultSet;

		FalkorDBQueryStatistics(com.falkordb.ResultSet resultSet) {
			this.resultSet = resultSet;
		}

		@Override
		public int nodesCreated() {
			return this.resultSet.getStatistics().nodesCreated();
		}

		@Override
		public int nodesDeleted() {
			return this.resultSet.getStatistics().nodesDeleted();
		}

		@Override
		public int relationshipsCreated() {
			return this.resultSet.getStatistics().relationshipsCreated();
		}

		@Override
		public int relationshipsDeleted() {
			return this.resultSet.getStatistics().relationshipsDeleted();
		}

		@Override
		public int propertiesSet() {
			return this.resultSet.getStatistics().propertiesSet();
		}

		@Override
		public int labelsAdded() {
			return this.resultSet.getStatistics().labelsAdded();
		}

		@Override
		public int labelsRemoved() {
			// This method is not available in JFalkorDB Statistics interface
			return 0;
		}

	}

}
