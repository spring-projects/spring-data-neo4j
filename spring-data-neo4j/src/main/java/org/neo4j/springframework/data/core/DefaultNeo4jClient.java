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
import static org.neo4j.springframework.data.core.Neo4jClient.*;
import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager.*;
import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionUtils.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.convert.Neo4jConversions;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link Neo4jClient}. Uses the Neo4j Java driver to connect to and interact with the database.
 * TODO Micrometer hooks for statement results...
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
class DefaultNeo4jClient implements Neo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;
	private final ConversionService conversionService;

	DefaultNeo4jClient(Driver driver) {

		this.driver = driver;
		this.typeSystem = driver.defaultTypeSystem();

		this.conversionService = new DefaultConversionService();
		new Neo4jConversions().registerConvertersIn((ConverterRegistry) conversionService);
	}

	AutoCloseableQueryRunner getQueryRunner(@Nullable final String targetDatabase) {

		QueryRunner queryRunner = retrieveTransaction(driver, targetDatabase);
		if (queryRunner == null) {
			queryRunner = driver.session(defaultSessionConfig(targetDatabase));
		}

		return (AutoCloseableQueryRunner) Proxy.newProxyInstance(this.getClass().getClassLoader(),
			new Class<?>[] { AutoCloseableQueryRunner.class },
			new AutoCloseableQueryRunnerHandler(queryRunner));
	}

	/**
	 * Makes a query runner automatically closeable and aware whether it's session or a transaction
	 */
	interface AutoCloseableQueryRunner extends QueryRunner, AutoCloseable {

		@Override void close();
	}

	static class AutoCloseableQueryRunnerHandler implements InvocationHandler {

		private final Map<Method, MethodHandle> cachedHandles = new ConcurrentHashMap<>();
		private final QueryRunner target;

		AutoCloseableQueryRunnerHandler(QueryRunner target) {
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
				return cachedHandles.computeIfAbsent(method, this::findHandleFor).invokeWithArguments(args);
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
	public <T> OngoingDelegation<T> delegateTo(Function<QueryRunner, Optional<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
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

		protected final Result runWith(AutoCloseableQueryRunner statementRunner) {
			String statementTemplate = cypherSupplier.get();

			if (cypherLog.isDebugEnabled()) {
				cypherLog.debug(() -> String.format("Executing:%s%s", System.lineSeparator(), statementTemplate));

				if (cypherLog.isTraceEnabled() && !parameters.isEmpty()) {
					cypherLog.trace(() -> String.format("with parameters:%s%s", System.lineSeparator(), parameters));
				}
			}

			return statementRunner.run(statementTemplate, parameters.get());
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

			this.targetDatabase = verifyDatabaseName(targetDatabase);
			return this;
		}

		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, RunnableSpecTightToDatabase> {

			@Nullable
			private final T value;

			DefaultOngoingBindSpec(@Nullable T value) {
				this.value = value;
			}

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
		public <T> MappingSpec<T> fetchAs(Class<T> targetClass) {

			return new DefaultRecordFetchSpec(this.targetDatabase, this.runnableStatement,
				new SingleValueMappingFunction(conversionService, targetClass));
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(
				this.targetDatabase,
				this.runnableStatement, (t, r) -> r.asMap());
		}

		@Override
		public ResultSummary run() {

			try (AutoCloseableQueryRunner statementRunner = getQueryRunner(this.targetDatabase)) {
				Result result = runnableStatement.runWith(statementRunner);
				return result.consume();
			}
		}
	}

	class DefaultRecordFetchSpec<T> implements RecordFetchSpec<T>, MappingSpec<T> {

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
		public RecordFetchSpec<T> mappedBy(
			@SuppressWarnings("HiddenField") BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = new DelegatingMappingFunctionWithNullCheck<>(mappingFunction);
			return this;
		}

		@Override
		public Optional<T> one() {

			try (AutoCloseableQueryRunner statementRunner = getQueryRunner(this.targetDatabase)) {
				Result result = runnableStatement.runWith(statementRunner);
				return result.hasNext() ?
					Optional.of(mappingFunction.apply(typeSystem, result.single())) :
					Optional.empty();
			}
		}

		@Override
		public Optional<T> first() {

			try (AutoCloseableQueryRunner statementRunner = getQueryRunner(this.targetDatabase)) {
				Result result = runnableStatement.runWith(statementRunner);
				return result.stream().map(partialMappingFunction(typeSystem)).findFirst();
			}
		}

		@Override
		public Collection<T> all() {

			try (AutoCloseableQueryRunner statementRunner = getQueryRunner(this.targetDatabase)) {
				Result result = runnableStatement.runWith(statementRunner);
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

	class DefaultRunnableDelegation<T> implements RunnableDelegation<T>, OngoingDelegation<T> {

		private final Function<QueryRunner, Optional<T>> callback;

		@Nullable private String targetDatabase;

		DefaultRunnableDelegation(Function<QueryRunner, Optional<T>> callback) {
			this(callback, null);
		}

		DefaultRunnableDelegation(Function<QueryRunner, Optional<T>> callback, @Nullable String targetDatabase) {
			this.callback = callback;
			this.targetDatabase = targetDatabase;
		}

		@Override
		public RunnableDelegation in(@Nullable @SuppressWarnings("HiddenField") String targetDatabase) {

			this.targetDatabase = verifyDatabaseName(targetDatabase);
			return this;
		}

		@Override
		public Optional<T> run() {
			try (AutoCloseableQueryRunner queryRunner = getQueryRunner(targetDatabase)) {
				return callback.apply(queryRunner);
			}
		}
	}
}
