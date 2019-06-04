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

import static java.util.stream.Collectors.*;
import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager.*;
import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionUtils.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.StatementRunner;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.neo4j.springframework.data.repository.NoResultException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link Neo4jClient}. Uses the Neo4j Java driver to connect to and interact with the database.
 * TODO Micrometer hooks for statement results...
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class DefaultNeo4jClient implements Neo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;

	DefaultNeo4jClient(Driver driver) {

		this.driver = driver;
		this.typeSystem = driver.defaultTypeSystem();
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
	 * Makes a statement runner automatically closeable and aware whether it's session or a transaction
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
	public RunnableSpec query(String cypher) {
		return query(() -> cypher);
	}

	@Override
	public RunnableSpec query(Supplier<String> cypherSupplier) {
		return new DefaultRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingDelegation<T> delegateTo(Function<StatementRunner, Optional<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		Neo4jClient.MappingSpec<Optional<T>, Collection<T>, T> mappingSpec = this
			.query(preparedQuery.getCypherQuery())
			.bindAll(preparedQuery.getParameters())
			.fetchAs(preparedQuery.getResultType());
		Neo4jClient.RecordFetchSpec<Optional<T>, Collection<T>, T> fetchSpec = preparedQuery
			.getOptionalMappingFunction()
			.map(f -> mappingSpec.mappedBy(f))
			.orElse(mappingSpec);

		return new DefaultExecutableQuery<>(preparedQuery, fetchSpec);
	}

	/**
	 * Basically a holder of a cypher template supplier and a set of named parameters. It's main purpose is to
	 * orchestrate the running of things with a bit of logging.
	 */
	class RunnableStatement {

		RunnableStatement(Supplier<String> cypherSupplier) {
			this(cypherSupplier, new NamedParameters());
		}

		RunnableStatement(Supplier<String> cypherSupplier, NamedParameters parameters) {
			this.cypherSupplier = cypherSupplier;
			this.parameters = parameters;
		}

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		protected final StatementResult runWith(AutoCloseableStatementRunner statementRunner) {
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

	class DefaultRunnableSpec implements RunnableSpec {

		private RunnableStatement runnableStatement;

		private String targetDatabase;

		DefaultRunnableSpec(Supplier<String> cypherSupplier) {
			this.runnableStatement = new RunnableStatement(cypherSupplier);
		}

		@Override
		public RunnableSpecTightToDatabase in(@SuppressWarnings("HiddenField") String targetDatabase) {
			this.targetDatabase = targetDatabase;
			return this;
		}

		@RequiredArgsConstructor
		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, RunnableSpecTightToDatabase> {

			@Nullable
			private final T value;

			@Override
			public RunnableSpecTightToDatabase to(String name) {

				DefaultRunnableSpec.this.runnableStatement.parameters.add(name, value);
				return DefaultRunnableSpec.this;
			}

			@Override
			public RunnableSpecTightToDatabase with(Function<T, Map<String, Object>> binder) {

				Assert.notNull(binder, "Binder is required.");

				return bindAll(binder.apply(value));
			}
		}

		@Override
		public OngoingBindSpec<?, RunnableSpecTightToDatabase> bind(@Nullable Object value) {
			return new DefaultOngoingBindSpec(value);
		}

		@Override
		public RunnableSpecTightToDatabase bindAll(Map<String, Object> newParameters) {
			this.runnableStatement.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <T> MappingSpec<Optional<T>, Collection<T>, T> fetchAs(Class<T> targetClass) {

			return new DefaultRecordFetchSpec(this.targetDatabase, this.runnableStatement,
				new SingleValueMappingFunction(targetClass));
		}

		@Override
		public RecordFetchSpec<Optional<Map<String, Object>>, Collection<Map<String, Object>>, Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(
				this.targetDatabase,
				this.runnableStatement, (t, r) -> r.asMap());
		}

		@Override
		public ResultSummary run() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runnableStatement.runWith(statementRunner);
				return result.consume();
			}
		}
	}

	class DefaultRecordFetchSpec<T>
		implements RecordFetchSpec<Optional<T>, Collection<T>, T>, MappingSpec<Optional<T>, Collection<T>, T> {

		private final String targetDatabase;

		private final RunnableStatement runnableStatement;

		private BiFunction<TypeSystem, Record, T> mappingFunction;

		DefaultRecordFetchSpec(String targetDatabase, RunnableStatement runnableStatement,
			BiFunction<TypeSystem, Record, T> mappingFunction) {
			this.targetDatabase = targetDatabase;
			this.runnableStatement = runnableStatement;
			this.mappingFunction = mappingFunction;
		}

		@Override
		public RecordFetchSpec<Optional<T>, Collection<T>, T> mappedBy(
			@SuppressWarnings("HiddenField") BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = new DelegatingMappingFunctionWithNullCheck<>(mappingFunction);
			return this;
		}

		@Override
		public Optional<T> one() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runnableStatement.runWith(statementRunner);
				return result.hasNext() ?
					Optional.of(mappingFunction.apply(typeSystem, result.single())) :
					Optional.empty();
			}
		}

		@Override
		public Optional<T> first() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runnableStatement.runWith(statementRunner);
				return result.stream().map(partialMappingFunction(typeSystem)).findFirst();
			}
		}

		@Override
		public Collection<T> all() {

			try (AutoCloseableStatementRunner statementRunner = getStatementRunner(this.targetDatabase)) {
				StatementResult result = runnableStatement.runWith(statementRunner);
				return result.stream().map(partialMappingFunction(typeSystem)).collect(toList());
			}
		}

		/**
		 * @param typeSystem The actual type system
		 * @return The partially evaluated mapping function
		 */
		private Function<Record, T> partialMappingFunction(TypeSystem typeSystem) {
			return r -> mappingFunction.apply(typeSystem, r);
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

	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	final class DefaultExecutableQuery<T> implements ExecutableQuery<T> {

		private final PreparedQuery<T> preparedQuery;
		private final Neo4jClient.RecordFetchSpec<Optional<T>, Collection<T>, T> fetchSpec;

		public List<T> getResults() {
			return fetchSpec.all().stream().collect(toList());
		}

		public Optional<T> getSingleResult() {
			try {
				return fetchSpec.one();
			} catch (NoSuchRecordException e) {
				// This exception is thrown by the driver in both cases when there are 0 or 1+n records
				// So there has been an incorrect result size, but not to few results but to many.
				throw new IncorrectResultSizeDataAccessException(1);
			}
		}

		public T getRequiredSingleResult() {
			return fetchSpec.one()
				.orElseThrow(() -> new NoResultException(1, preparedQuery.getCypherQuery()));
		}
	}

}
