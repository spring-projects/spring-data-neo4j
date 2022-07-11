/*
 * Copyright 2011-2022 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link Neo4jClient}. Uses the Neo4j Java driver to connect to and interact with the
 * database.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class DefaultNeo4jClient implements Neo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;
	private @Nullable final DatabaseSelectionProvider databaseSelectionProvider;
	private @Nullable final UserSelectionProvider userSelectionProvider;
	private final ConversionService conversionService;
	private final Neo4jPersistenceExceptionTranslator persistenceExceptionTranslator = new Neo4jPersistenceExceptionTranslator();

	// Basically a local bookmark manager
	private final Set<Bookmark> bookmarks = new HashSet<>();
	private final ReentrantReadWriteLock bookmarksLock = new ReentrantReadWriteLock();

	DefaultNeo4jClient(Builder builder) {

		this.driver = builder.driver;
		this.typeSystem = driver.defaultTypeSystem();
		this.databaseSelectionProvider = builder.databaseSelectionProvider;
		this.userSelectionProvider = builder.userSelectionProvider;

		this.conversionService = new DefaultConversionService();
		Optional.ofNullable(builder.neo4jConversions).orElseGet(Neo4jConversions::new).registerConvertersIn((ConverterRegistry) conversionService);
	}

	@Override
	public QueryRunner getQueryRunner(DatabaseSelection databaseSelection, UserSelection impersonatedUser) {

		QueryRunner queryRunner = Neo4jTransactionManager.retrieveTransaction(driver, databaseSelection, impersonatedUser);
		Collection<Bookmark> lastBookmarks = Collections.emptySet();
		if (queryRunner == null) {
			ReentrantReadWriteLock.ReadLock lock = bookmarksLock.readLock();
			try {
				lock.lock();
				lastBookmarks = new HashSet<>(bookmarks);
				queryRunner = driver.session(Neo4jTransactionUtils.sessionConfig(false, lastBookmarks, databaseSelection, impersonatedUser));
			} finally {
				lock.unlock();
			}
		}

		return new DelegatingQueryRunner(queryRunner, lastBookmarks, (usedBookmarks, newBookmarks) -> {

			ReentrantReadWriteLock.WriteLock lock = bookmarksLock.writeLock();
			try {
				lock.lock();
				bookmarks.removeAll(usedBookmarks);
				bookmarks.addAll(newBookmarks);
			} finally {
				lock.unlock();
			}
		});
	}

	private static class DelegatingQueryRunner implements QueryRunner {

		private final QueryRunner delegate;
		private final Collection<Bookmark> usedBookmarks;
		private final BiConsumer<Collection<Bookmark>, Collection<Bookmark>> newBookmarkConsumer;

		private DelegatingQueryRunner(QueryRunner delegate, Collection<Bookmark> lastBookmarks, BiConsumer<Collection<Bookmark>, Collection<Bookmark>> newBookmarkConsumer) {
			this.delegate = delegate;
			this.usedBookmarks = lastBookmarks;
			this.newBookmarkConsumer = newBookmarkConsumer;
		}

		@Override
		public void close() {

			// We're only going to close sessions we have acquired inside the client, not something that
			// has been retrieved from the tx manager.
			if (this.delegate instanceof Session session) {

				session.close();
				this.newBookmarkConsumer.accept(usedBookmarks, session.lastBookmarks());
			}
		}

		@Override
		public Result run(String s, Value value) {
			return delegate.run(s, value);
		}

		@Override
		public Result run(String s, Map<String, Object> map) {
			return delegate.run(s, map);
		}

		@Override
		public Result run(String s, Record record) {
			return delegate.run(s, record);
		}

		@Override
		public Result run(String s) {
			return delegate.run(s);
		}

		@Override
		public Result run(Query query) {
			return delegate.run(query);
		}
	}

	// Below are all the implementations (methods and classes) as defined by the contracts of Neo4jClient

	@Override
	public UnboundRunnableSpec query(String cypher) {
		return query(() -> cypher);
	}

	@Override
	public UnboundRunnableSpec query(Supplier<String> cypherSupplier) {
		return new DefaultRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingDelegation<T> delegateTo(Function<QueryRunner, Optional<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
	}

	@Override
	@Nullable
	public DatabaseSelectionProvider getDatabaseSelectionProvider() {
		return databaseSelectionProvider;
	}

	/**
	 * Basically a holder of a cypher template supplier and a set of named parameters. It's main purpose is to orchestrate
	 * the running of things with a bit of logging.
	 */
	static class RunnableStatement {

		RunnableStatement(Supplier<String> cypherSupplier) {
			this(cypherSupplier, new NamedParameters());
		}

		RunnableStatement(Supplier<String> cypherSupplier, NamedParameters parameters) {
			this.cypherSupplier = cypherSupplier;
			this.parameters = parameters;
		}

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		protected final Result runWith(QueryRunner statementRunner) {
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

	/**
	 * Tries to convert the given {@link RuntimeException} into a {@link DataAccessException} but returns the original
	 * exception if the conversation failed. Thus allows safe re-throwing of the return value.
	 *
	 * @param ex                  the exception to translate
	 * @param exceptionTranslator the {@link PersistenceExceptionTranslator} to be used for translation
	 * @return Any translated exception
	 */
	private static RuntimeException potentiallyConvertRuntimeException(RuntimeException ex,
			PersistenceExceptionTranslator exceptionTranslator) {
		RuntimeException resolved = exceptionTranslator.translateExceptionIfPossible(ex);
		return resolved == null ? ex : resolved;
	}

	private DatabaseSelection resolveTargetDatabaseName(@Nullable String parameterTargetDatabase) {

		String value = Neo4jClient.verifyDatabaseName(parameterTargetDatabase);
		if (value != null) {
			return DatabaseSelection.byName(value);
		}
		if (databaseSelectionProvider != null) {
			return databaseSelectionProvider.getDatabaseSelection();
		}
		return DatabaseSelectionProvider.getDefaultSelectionProvider().getDatabaseSelection();
	}

	private UserSelection resolveUser(@Nullable String userName) {

		if (StringUtils.hasText(userName)) {
			return UserSelection.impersonate(userName);
		}
		if (userSelectionProvider != null) {
			return userSelectionProvider.getUserSelection();
		}
		return UserSelectionProvider.getDefaultSelectionProvider().getUserSelection();
	}

	class DefaultRunnableSpec implements UnboundRunnableSpec, RunnableSpecBoundToDatabaseAndUser {

		private final RunnableStatement runnableStatement;

		private DatabaseSelection databaseSelection;

		private UserSelection userSelection;

		DefaultRunnableSpec(Supplier<String> cypherSupplier) {

			this.databaseSelection = resolveTargetDatabaseName(null);
			this.userSelection = resolveUser(null);
			this.runnableStatement = new RunnableStatement(cypherSupplier);
		}

		@Override
		public RunnableSpecBoundToDatabase in(String targetDatabase) {

			this.databaseSelection = resolveTargetDatabaseName(targetDatabase);
			return new DefaultRunnableSpecBoundToDatabase();
		}

		@Override
		public RunnableSpecBoundToUser asUser(String asUser) {

			this.userSelection = resolveUser(asUser);
			return new DefaultRunnableSpecBoundToUser();
		}

		@Override
		public <T> OngoingBindSpec<T, RunnableSpec> bind(T value) {
			return new DefaultOngoingBindSpec<>(value);
		}

		@Override
		public RunnableSpec bindAll(Map<String, Object> newParameters) {
			this.runnableStatement.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <T> MappingSpec<T> fetchAs(Class<T> targetClass) {

			return new DefaultRecordFetchSpec<>(databaseSelection, userSelection, runnableStatement,
					new SingleValueMappingFunction<>(conversionService, targetClass));
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(databaseSelection, userSelection, runnableStatement, (t, r) -> r.asMap());
		}

		@Override
		public ResultSummary run() {

			try (QueryRunner statementRunner = getQueryRunner(databaseSelection, userSelection)) {
				Result result = runnableStatement.runWith(statementRunner);
				return ResultSummaries.process(result.consume());
			} catch (RuntimeException e) {
				throw potentiallyConvertRuntimeException(e, persistenceExceptionTranslator);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, RunnableSpec> {

			@Nullable private final T value;

			DefaultOngoingBindSpec(@Nullable T value) {
				this.value = value;
			}

			@Override
			public RunnableSpec to(String name) {

				DefaultRunnableSpec.this.runnableStatement.parameters.add(name, value);
				return DefaultRunnableSpec.this;
			}

			@Override
			public RunnableSpec with(Function<T, Map<String, Object>> binder) {

				Assert.notNull(binder, "Binder is required");

				return bindAll(binder.apply(value));
			}
		}

		class DefaultRunnableSpecBoundToDatabase implements RunnableSpecBoundToDatabase {
			@Override
			public RunnableSpecBoundToDatabaseAndUser asUser(String aUser) {

				DefaultRunnableSpec.this.userSelection = resolveUser(aUser);
				return DefaultRunnableSpec.this;
			}

			@Override
			public <T> MappingSpec<T> fetchAs(Class<T> targetClass) {
				return DefaultRunnableSpec.this.fetchAs(targetClass);
			}

			@Override
			public RecordFetchSpec<Map<String, Object>> fetch() {
				return DefaultRunnableSpec.this.fetch();
			}

			@Override
			public ResultSummary run() {
				return DefaultRunnableSpec.this.run();
			}

			@Override
			public <T> OngoingBindSpec<T, RunnableSpec> bind(T value) {
				return DefaultRunnableSpec.this.bind(value);
			}

			@Override
			public RunnableSpec bindAll(Map<String, Object> parameters) {
				return DefaultRunnableSpec.this.bindAll(parameters);
			}
		}

		class DefaultRunnableSpecBoundToUser implements RunnableSpecBoundToUser {

			@Override
			public RunnableSpecBoundToDatabaseAndUser in(String aDatabase) {

				DefaultRunnableSpec.this.databaseSelection = resolveTargetDatabaseName(aDatabase);
				return DefaultRunnableSpec.this;
			}

			@Override
			public <T> MappingSpec<T> fetchAs(Class<T> targetClass) {
				return DefaultRunnableSpec.this.fetchAs(targetClass);
			}

			@Override
			public RecordFetchSpec<Map<String, Object>> fetch() {
				return DefaultRunnableSpec.this.fetch();
			}

			@Override
			public ResultSummary run() {
				return DefaultRunnableSpec.this.run();
			}

			@Override
			public <T> OngoingBindSpec<T, RunnableSpec> bind(T value) {
				return DefaultRunnableSpec.this.bind(value);
			}

			@Override
			public RunnableSpec bindAll(Map<String, Object> parameters) {
				return DefaultRunnableSpec.this.bindAll(parameters);
			}
		}
	}

	class DefaultRecordFetchSpec<T> implements RecordFetchSpec<T>, MappingSpec<T> {

		private final DatabaseSelection databaseSelection;

		@Nullable
		private final UserSelection impersonatedUser;

		private final RunnableStatement runnableStatement;

		private BiFunction<TypeSystem, Record, T> mappingFunction;

		DefaultRecordFetchSpec(DatabaseSelection databaseSelection,
				@Nullable UserSelection impersonatedUser,
				RunnableStatement runnableStatement,
				BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.databaseSelection = databaseSelection;
			this.impersonatedUser = impersonatedUser;
			this.runnableStatement = runnableStatement;
			this.mappingFunction = mappingFunction;
		}

		@Override
		public RecordFetchSpec<T> mappedBy(
				@SuppressWarnings("HiddenField") BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = mappingFunction;
			return this;
		}

		@Override
		public Optional<T> one() {

			try (QueryRunner statementRunner = getQueryRunner(this.databaseSelection, this.impersonatedUser)) {
				Result result = runnableStatement.runWith(statementRunner);
				Optional<T> optionalValue = result.hasNext() ?
						Optional.ofNullable(mappingFunction.apply(typeSystem, result.single())) :
						Optional.empty();
				ResultSummaries.process(result.consume());
				return optionalValue;
			} catch (RuntimeException e) {
				throw potentiallyConvertRuntimeException(e, persistenceExceptionTranslator);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Optional<T> first() {

			try (QueryRunner statementRunner = getQueryRunner(this.databaseSelection, this.impersonatedUser)) {
				Result result = runnableStatement.runWith(statementRunner);
				Optional<T> optionalValue = result.stream().map(partialMappingFunction(typeSystem)).filter(Objects::nonNull).findFirst();
				ResultSummaries.process(result.consume());
				return optionalValue;
			} catch (RuntimeException e) {
				throw potentiallyConvertRuntimeException(e, persistenceExceptionTranslator);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Collection<T> all() {

			try (QueryRunner statementRunner = getQueryRunner(this.databaseSelection, this.impersonatedUser)) {
				Result result = runnableStatement.runWith(statementRunner);
				Collection<T> values = result.stream().map(partialMappingFunction(typeSystem)).filter(Objects::nonNull).collect(Collectors.toList());
				ResultSummaries.process(result.consume());
				return values;
			} catch (RuntimeException e) {
				throw potentiallyConvertRuntimeException(e, persistenceExceptionTranslator);
			} catch (Exception e) {
				throw new RuntimeException(e);
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

		private DatabaseSelection databaseSelection;

		@Nullable
		private UserSelection impersonatedUser;

		private final Function<QueryRunner, Optional<T>> callback;

		DefaultRunnableDelegation(Function<QueryRunner, Optional<T>> callback) {
			this.callback = callback;
			this.databaseSelection = resolveTargetDatabaseName(null);
			this.impersonatedUser = resolveUser(null);
		}

		@Override
		public RunnableDelegation<T> in(@Nullable String targetDatabase) {

			this.databaseSelection = resolveTargetDatabaseName(targetDatabase);
			return this;
		}

		@Override
		public Optional<T> run() {
			try (QueryRunner queryRunner = getQueryRunner(databaseSelection, this.impersonatedUser)) {
				return callback.apply(queryRunner);
			} catch (RuntimeException e) {
				throw potentiallyConvertRuntimeException(e, persistenceExceptionTranslator);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
