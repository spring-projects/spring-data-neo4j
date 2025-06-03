/*
 * Copyright 2011-2025 the original author or authors.
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.support.BookmarkManagerReference;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link Neo4jClient}. Uses the Neo4j Java driver to connect to
 * and interact with the database.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class DefaultNeo4jClient implements Neo4jClient, ApplicationContextAware {

	private final Driver driver;

	@Nullable
	private final DatabaseSelectionProvider databaseSelectionProvider;

	@Nullable
	private final UserSelectionProvider userSelectionProvider;

	private final ConversionService conversionService;

	private final Neo4jPersistenceExceptionTranslator persistenceExceptionTranslator = new Neo4jPersistenceExceptionTranslator();

	// Local bookmark manager when using outside managed transactions
	private final BookmarkManagerReference bookmarkManager;

	DefaultNeo4jClient(Builder builder) {

		this.driver = builder.driver;
		this.databaseSelectionProvider = builder.databaseSelectionProvider;
		this.userSelectionProvider = builder.userSelectionProvider;
		this.bookmarkManager = new BookmarkManagerReference(Neo4jBookmarkManager::create, builder.bookmarkManager);

		this.conversionService = new DefaultConversionService();
		Optional.ofNullable(builder.neo4jConversions)
			.orElseGet(Neo4jConversions::new)
			.registerConvertersIn((ConverterRegistry) this.conversionService);
	}

	/**
	 * Tries to convert the given {@link RuntimeException} into a
	 * {@link DataAccessException} but returns the original exception if the conversation
	 * failed. Thus allows safe re-throwing of the return value.
	 * @param ex the exception to translate
	 * @param exceptionTranslator the {@link PersistenceExceptionTranslator} to be used
	 * for translation
	 * @return any translated exception
	 */
	private static RuntimeException potentiallyConvertRuntimeException(RuntimeException ex,
			PersistenceExceptionTranslator exceptionTranslator) {
		RuntimeException resolved = exceptionTranslator.translateExceptionIfPossible(ex);
		return (resolved != null) ? resolved : ex;
	}

	@Override
	public QueryRunner getQueryRunner(DatabaseSelection databaseSelection, UserSelection impersonatedUser) {

		QueryRunner queryRunner = Neo4jTransactionManager.retrieveTransaction(this.driver, databaseSelection,
				impersonatedUser);
		Collection<Bookmark> lastBookmarks = this.bookmarkManager.resolve().getBookmarks();

		if (queryRunner == null) {
			queryRunner = this.driver.session(
					Neo4jTransactionUtils.sessionConfig(false, lastBookmarks, databaseSelection, impersonatedUser));
		}

		return new DelegatingQueryRunner(queryRunner, lastBookmarks, this.bookmarkManager.resolve()::updateBookmarks);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.bookmarkManager.setApplicationContext(applicationContext);
	}

	// Below are all the implementations (methods and classes) as defined by the contracts
	// of Neo4jClient

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
	@Nullable public DatabaseSelectionProvider getDatabaseSelectionProvider() {
		return this.databaseSelectionProvider;
	}

	private DatabaseSelection resolveTargetDatabaseName(@Nullable String parameterTargetDatabase) {

		String value = Neo4jClient.verifyDatabaseName(parameterTargetDatabase);
		if (value != null) {
			return DatabaseSelection.byName(value);
		}
		if (this.databaseSelectionProvider != null) {
			return this.databaseSelectionProvider.getDatabaseSelection();
		}
		return DatabaseSelectionProvider.getDefaultSelectionProvider().getDatabaseSelection();
	}

	private UserSelection resolveUser(@Nullable String userName) {

		if (StringUtils.hasText(userName)) {
			return UserSelection.impersonate(userName);
		}
		if (this.userSelectionProvider != null) {
			return this.userSelectionProvider.getUserSelection();
		}
		return UserSelectionProvider.getDefaultSelectionProvider().getUserSelection();
	}

	private static final class DelegatingQueryRunner implements QueryRunner {

		private final QueryRunner delegate;

		private final Collection<Bookmark> usedBookmarks;

		private final BiConsumer<Collection<Bookmark>, Collection<Bookmark>> newBookmarkConsumer;

		private DelegatingQueryRunner(QueryRunner delegate, Collection<Bookmark> lastBookmarks,
				BiConsumer<Collection<Bookmark>, Collection<Bookmark>> newBookmarkConsumer) {
			this.delegate = delegate;
			this.usedBookmarks = lastBookmarks;
			this.newBookmarkConsumer = newBookmarkConsumer;
		}

		@Override
		public void close() {

			// We're only going to close sessions we have acquired inside the client, not
			// something that
			// has been retrieved from the tx manager.
			if (this.delegate instanceof Session session) {

				session.close();
				this.newBookmarkConsumer.accept(this.usedBookmarks, session.lastBookmarks());
			}
		}

		@Override
		public Result run(String s, Value value) {
			return this.delegate.run(s, value);
		}

		@Override
		public Result run(String s, Map<String, Object> map) {
			return this.delegate.run(s, map);
		}

		@Override
		public Result run(String s, Record record) {
			return this.delegate.run(s, record);
		}

		@Override
		public Result run(String s) {
			return this.delegate.run(s);
		}

		@Override
		public Result run(Query query) {
			return this.delegate.run(query);
		}

	}

	/**
	 * Basically a holder of a cypher template supplier and a set of named parameters.
	 * It's main purpose is to orchestrate the running of things with a bit of logging.
	 */
	static class RunnableStatement {

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		RunnableStatement(Supplier<String> cypherSupplier) {
			this(cypherSupplier, new NamedParameters());
		}

		RunnableStatement(Supplier<String> cypherSupplier, NamedParameters parameters) {
			this.cypherSupplier = cypherSupplier;
			this.parameters = parameters;
		}

		protected final Result runWith(QueryRunner statementRunner) {
			String statementTemplate = this.cypherSupplier.get();

			if (cypherLog.isDebugEnabled()) {
				cypherLog.debug(() -> String.format("Executing:%s%s", System.lineSeparator(), statementTemplate));

				if (cypherLog.isTraceEnabled() && !this.parameters.isEmpty()) {
					cypherLog
						.trace(() -> String.format("with parameters:%s%s", System.lineSeparator(), this.parameters));
				}
			}

			return statementRunner.run(statementTemplate, this.parameters.get());
		}

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
		public <T> OngoingBindSpec<T, RunnableSpec> bind(@Nullable T value) {
			return new DefaultOngoingBindSpec<>(value);
		}

		@Override
		public RunnableSpec bindAll(Map<String, Object> newParameters) {
			this.runnableStatement.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <T> MappingSpec<T> fetchAs(Class<T> targetClass) {

			return new DefaultRecordFetchSpec<>(this.databaseSelection, this.userSelection, this.runnableStatement,
					new SingleValueMappingFunction<>(DefaultNeo4jClient.this.conversionService, targetClass));
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(this.databaseSelection, this.userSelection, this.runnableStatement,
					(t, r) -> r.asMap());
		}

		@Override
		public ResultSummary run() {

			try (QueryRunner statementRunner = getQueryRunner(this.databaseSelection, this.userSelection)) {
				Result result = this.runnableStatement.runWith(statementRunner);
				return ResultSummaries.process(result.consume());
			}
			catch (RuntimeException ex) {
				throw potentiallyConvertRuntimeException(ex, DefaultNeo4jClient.this.persistenceExceptionTranslator);
			}
			catch (Exception exception) {
				throw new RuntimeException(exception);
			}
		}

		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, RunnableSpec> {

			@Nullable
			private final T value;

			DefaultOngoingBindSpec(@Nullable T value) {
				this.value = value;
			}

			@Override
			public RunnableSpec to(String name) {

				DefaultRunnableSpec.this.runnableStatement.parameters.add(name, this.value);
				return DefaultRunnableSpec.this;
			}

			@Override
			public RunnableSpec with(Function<T, Map<String, Object>> binder) {

				Assert.notNull(binder, "Binder is required");

				return bindAll(binder.apply(this.value));
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
			public <T> OngoingBindSpec<T, RunnableSpec> bind(@Nullable T value) {
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
			public <T> OngoingBindSpec<T, RunnableSpec> bind(@Nullable T value) {
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

		private final UserSelection impersonatedUser;

		private final RunnableStatement runnableStatement;

		private BiFunction<TypeSystem, Record, T> mappingFunction;

		DefaultRecordFetchSpec(DatabaseSelection databaseSelection, UserSelection impersonatedUser,
				RunnableStatement runnableStatement, BiFunction<TypeSystem, Record, T> mappingFunction) {

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
				Result result = this.runnableStatement.runWith(statementRunner);
				Optional<T> optionalValue = result.hasNext()
						? Optional.ofNullable(this.mappingFunction.apply(TypeSystem.getDefault(), result.single()))
						: Optional.empty();
				ResultSummaries.process(result.consume());
				return optionalValue;
			}
			catch (RuntimeException ex) {
				throw potentiallyConvertRuntimeException(ex, DefaultNeo4jClient.this.persistenceExceptionTranslator);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public Optional<T> first() {

			try (QueryRunner statementRunner = getQueryRunner(this.databaseSelection, this.impersonatedUser)) {
				Result result = this.runnableStatement.runWith(statementRunner);
				Optional<T> optionalValue = result.stream()
					.map(partialMappingFunction(TypeSystem.getDefault()))
					.filter(Objects::nonNull)
					.findFirst();
				ResultSummaries.process(result.consume());
				return optionalValue;
			}
			catch (RuntimeException ex) {
				throw potentiallyConvertRuntimeException(ex, DefaultNeo4jClient.this.persistenceExceptionTranslator);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		@Override
		public Collection<T> all() {

			try (QueryRunner statementRunner = getQueryRunner(this.databaseSelection, this.impersonatedUser)) {
				Result result = this.runnableStatement.runWith(statementRunner);
				Collection<T> values = result.stream().flatMap(r -> {
					if (this.mappingFunction instanceof SingleValueMappingFunction && r.size() == 1
							&& r.get(0).hasType(TypeSystem.getDefault().LIST())) {
						return r.get(0)
							.asList(v -> ((SingleValueMappingFunction<T>) this.mappingFunction).convertValue(v))
							.stream();
					}
					return Stream.of(partialMappingFunction(TypeSystem.getDefault()).apply(r));
				}).filter(Objects::nonNull).collect(Collectors.toList());
				ResultSummaries.process(result.consume());
				return values;
			}
			catch (RuntimeException ex) {
				throw potentiallyConvertRuntimeException(ex, DefaultNeo4jClient.this.persistenceExceptionTranslator);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

		private Function<Record, T> partialMappingFunction(TypeSystem typeSystem) {
			return r -> this.mappingFunction.apply(typeSystem, r);
		}

	}

	class DefaultRunnableDelegation<T> implements RunnableDelegation<T>, OngoingDelegation<T> {

		private final Function<QueryRunner, Optional<T>> callback;

		private DatabaseSelection databaseSelection;

		private UserSelection impersonatedUser;

		DefaultRunnableDelegation(Function<QueryRunner, Optional<T>> callback) {
			this.callback = callback;
			this.databaseSelection = resolveTargetDatabaseName(null);
			this.impersonatedUser = resolveUser(null);
		}

		@Override
		public RunnableDelegation<T> in(String targetDatabase) {

			this.databaseSelection = resolveTargetDatabaseName(targetDatabase);
			return this;
		}

		@Override
		public Optional<T> run() {
			try (QueryRunner queryRunner = getQueryRunner(this.databaseSelection, this.impersonatedUser)) {
				return this.callback.apply(queryRunner);
			}
			catch (RuntimeException ex) {
				throw potentiallyConvertRuntimeException(ex, DefaultNeo4jClient.this.persistenceExceptionTranslator);
			}
			catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}

	}

}
