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

import static java.util.stream.Collectors.*;
import static org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils.*;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.StatementRunner;
import org.neo4j.driver.summary.ResultSummary;

/**
 * Default implementation of {@link Neo4jClient}. Uses the Neo4j Java driver to connect to and interact with the database.
 * TODO Micrometer hooks for statement results...
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class DefaultNeo4jClient implements Neo4jClient {

	private final Driver driver;

	DefaultNeo4jClient(Driver driver) {

		this.driver = driver;
	}

	AutoCloseableStatementRunner getStatementRunner(final String targetDatabase) {

		StatementRunner statementRunner = retrieveTransaction(driver, targetDatabase)
			.map(StatementRunner.class::cast)
			.orElseGet(() -> driver.session(defaultSessionParameters(targetDatabase)));

		return (AutoCloseableStatementRunner) Proxy.newProxyInstance(StatementRunner.class.getClassLoader(),
			new Class<?>[] { AutoCloseableStatementRunner.class },
			new AutoCloseableStatementRunnerHandler(statementRunner));
	}

	/**
	 * Makes a statement runner autoclosable and aware wether it's session or a transaction
	 */
	interface AutoCloseableStatementRunner extends StatementRunner, AutoCloseable {

		@Override void close();
	}

	static class AutoCloseableStatementRunnerHandler implements InvocationHandler {

		private final Map<Method, MethodHandle> cachedHandles = new ConcurrentHashMap<>();
		private final StatementRunner target;

		AutoCloseableStatementRunnerHandler(StatementRunner target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			if ("close".equals(method.getName())) {
				if (this.target instanceof Session) {
					((Session) this.target).close();
				}
				return null;
			} else {
				return cachedHandles.computeIfAbsent(method, this::findHandleFor)
					.invokeWithArguments(args);
			}
		}

		MethodHandle findHandleFor(Method method) {
			try {
				return MethodHandles.publicLookup().unreflect(method).bindTo(target);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// Below are all the implementations (methods and classes) as defined by the contracts of Neo4jClient

	@Override
	public RunnableSpec newQuery(String cypher) {
		return newQuery(() -> cypher);
	}

	@Override
	public RunnableSpec newQuery(Supplier<String> cypherSupplier) {
		return new DefaultRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingDelegation<T> delegateTo(Function<StatementRunner, Optional<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
	}

	@RequiredArgsConstructor
	class DefaultRunnableSpec implements RunnableSpec {

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters = new NamedParameters();

		private String targetDatabase;

		@Override
		public RunnableSpecTightToDatabase in(@SuppressWarnings("HiddenField") String targetDatabase) {
			this.targetDatabase = targetDatabase;
			return this;
		}

		@RequiredArgsConstructor
		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, RunnableSpecTightToDatabase> {

			private final T value;

			@Override
			public RunnableSpecTightToDatabase to(String name) {

				DefaultRunnableSpec.this.parameters.add(name, value);
				return DefaultRunnableSpec.this;
			}

			@Override
			public RunnableSpecTightToDatabase with(Function<T, Map<String, Object>> binder) {
				return bindAll(binder.apply(value));
			}
		}

		@Override
		public OngoingBindSpec<?, RunnableSpecTightToDatabase> bind(Object value) {
			return new DefaultOngoingBindSpec(value);
		}

		@Override
		public RunnableSpecTightToDatabase bindAll(Map<String, Object> newParameters) {
			this.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <T> MappingSpec<Optional<T>, Collection<T>, T> fetchAs(Class<T> targetClass) {

			return new DefaultRecordFetchSpec(this.targetDatabase, this.cypherSupplier, this.parameters,
				new SingleValueMappingFunction(targetClass));
		}

		@Override
		public RecordFetchSpec<Optional<Map<String, Object>>, Collection<Map<String, Object>>, Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(
				this.targetDatabase,
				this.cypherSupplier,
				this.parameters, Record::asMap);
		}

		@Override
		public ResultSummary run() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = statementRunner.run(cypherSupplier.get(), parameters.get());
				return result.consume();
			}
		}
	}

	@AllArgsConstructor
	class DefaultRecordFetchSpec<T>
		implements RecordFetchSpec<Optional<T>, Collection<T>, T>, MappingSpec<Optional<T>, Collection<T>, T> {

		private final String targetDatabase;

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		private Function<Record, T> mappingFunction;

		@Override
		public RecordFetchSpec<Optional<T>, Collection<T>, T> mappedBy(
			@SuppressWarnings("HiddenField") Function<Record, T> mappingFunction) {

			this.mappingFunction = new DelegatingMappingFunctionWithNullCheck<>(mappingFunction);
			return this;
		}

		@Override
		public Optional<T> one() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runWith(statementRunner);
				return result.hasNext() ? Optional.of(mappingFunction.apply(result.single())) : Optional.empty();
			}
		}

		@Override
		public Optional<T> first() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runWith(statementRunner);
				return result.stream().map(mappingFunction).findFirst();
			}
		}

		@Override
		public Collection<T> all() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runWith(statementRunner);
				return result.stream().map(mappingFunction).collect(toList());
			}
		}

		private StatementResult runWith(AutoCloseableStatementRunner statementRunner) {
			String statementTemplate = cypherSupplier.get();

			if (cypherLog.isDebugEnabled()) {
				cypherLog.debug("Executing:{}{}", System.lineSeparator(), statementTemplate);

				if (cypherLog.isTraceEnabled() && !parameters.isEmpty()) {
					cypherLog.trace("with parameters:{}{}", System.lineSeparator(), parameters);
				}
			}

			StatementResult result = statementRunner.run(statementTemplate, parameters.get());
			return result;
		}
	}

	@AllArgsConstructor
	@RequiredArgsConstructor
	class DefaultRunnableDelegation<T> implements RunnableDelegation<T>, OngoingDelegation<T> {

		private final Function<StatementRunner, Optional<T>> callback;

		private String targetDatabase;

		@Override
		public RunnableDelegation in(@SuppressWarnings("HiddenField") String targetDatabase) {
			this.targetDatabase = targetDatabase;
			return this;
		}

		@Override
		public Optional<T> run() {
			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(targetDatabase)) {
				return callback.apply(statementRunner);
			}
		}
	}
}
