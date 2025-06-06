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

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.reactivestreams.ReactiveQueryRunner;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.support.BookmarkManagerReference;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Reactive variant of the {@link Neo4jClient}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 6.0
 */
final class DefaultReactiveNeo4jClient implements ReactiveNeo4jClient, ApplicationContextAware {

	private final Driver driver;

	@Nullable
	private final ReactiveDatabaseSelectionProvider databaseSelectionProvider;

	@Nullable
	private final ReactiveUserSelectionProvider userSelectionProvider;

	private final ConversionService conversionService;

	private final Neo4jPersistenceExceptionTranslator persistenceExceptionTranslator = new Neo4jPersistenceExceptionTranslator();

	// Local bookmark manager when using outside managed transactions
	private final BookmarkManagerReference bookmarkManager;

	DefaultReactiveNeo4jClient(Builder builder) {

		this.driver = builder.driver;
		this.databaseSelectionProvider = builder.databaseSelectionProvider;
		this.userSelectionProvider = builder.impersonatedUserProvider;

		this.conversionService = new DefaultConversionService();
		Optional.ofNullable(builder.neo4jConversions)
			.orElseGet(Neo4jConversions::new)
			.registerConvertersIn((ConverterRegistry) this.conversionService);
		this.bookmarkManager = new BookmarkManagerReference(Neo4jBookmarkManager::createReactive,
				builder.bookmarkManager);
	}

	@Override
	public Mono<ReactiveQueryRunner> getQueryRunner(Mono<DatabaseSelection> databaseSelection,
			Mono<UserSelection> userSelection) {

		return databaseSelection.zipWith(userSelection)
			.flatMap(targetDatabaseAndUser -> ReactiveNeo4jTransactionManager
				.retrieveReactiveTransaction(this.driver, targetDatabaseAndUser.getT1(), targetDatabaseAndUser.getT2())
				.map(ReactiveQueryRunner.class::cast)
				.zipWith(Mono.just(this.bookmarkManager.resolve().getBookmarks()))
				.switchIfEmpty(Mono.fromSupplier(() -> {
					Collection<Bookmark> lastBookmarks = this.bookmarkManager.resolve().getBookmarks();
					return Tuples.of(
							this.driver.session(ReactiveSession.class,
									Neo4jTransactionUtils.sessionConfig(false, lastBookmarks,
											targetDatabaseAndUser.getT1(), targetDatabaseAndUser.getT2())),
							lastBookmarks);
				})))
			.map(t -> new DelegatingQueryRunner(t.getT1(), t.getT2(), this.bookmarkManager.resolve()::updateBookmarks));
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.bookmarkManager.setApplicationContext(applicationContext);
	}

	<T> Mono<T> doInQueryRunnerForMono(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection,
			Function<ReactiveQueryRunner, Mono<T>> func) {

		return Mono.usingWhen(getQueryRunner(databaseSelection, userSelection), func,
				runner -> ((DelegatingQueryRunner) runner).close());
	}

	<T> Flux<T> doInStatementRunnerForFlux(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection,
			Function<ReactiveQueryRunner, Flux<T>> func) {

		return Flux.usingWhen(getQueryRunner(databaseSelection, userSelection), func,
				runner -> ((DelegatingQueryRunner) runner).close());
	}

	@Override
	public UnboundRunnableSpec query(String cypher) {
		return query(() -> cypher);
	}

	@Override
	public UnboundRunnableSpec query(Supplier<String> cypherSupplier) {
		return new DefaultRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingDelegation<T> delegateTo(Function<ReactiveQueryRunner, Mono<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
	}

	@Override
	@Nullable public ReactiveDatabaseSelectionProvider getDatabaseSelectionProvider() {
		return this.databaseSelectionProvider;
	}

	private Mono<DatabaseSelection> resolveTargetDatabaseName(@Nullable String parameterTargetDatabase) {

		String value = Neo4jClient.verifyDatabaseName(parameterTargetDatabase);
		if (value != null) {
			return Mono.just(DatabaseSelection.byName(value));
		}
		return Objects
			.requireNonNullElseGet(this.databaseSelectionProvider,
					ReactiveDatabaseSelectionProvider::getDefaultSelectionProvider)
			.getDatabaseSelection();
	}

	private Mono<UserSelection> resolveUser(@Nullable String userName) {

		if (StringUtils.hasText(userName)) {
			return Mono.just(UserSelection.impersonate(userName));
		}
		return Objects
			.requireNonNullElseGet(this.userSelectionProvider,
					ReactiveUserSelectionProvider::getDefaultSelectionProvider)
			.getUserSelection();
	}

	/**
	 * Tries to convert the given {@link RuntimeException} into a
	 * {@link DataAccessException} but returns the original exception if the conversation
	 * failed. Thus allows safe re-throwing of the return value.
	 * @param ex the exception to translate
	 * @return any translated exception
	 */
	private RuntimeException potentiallyConvertRuntimeException(RuntimeException ex) {
		RuntimeException resolved = this.persistenceExceptionTranslator.translateExceptionIfPossible(ex);
		return (resolved != null) ? resolved : ex;
	}

	private static final class DelegatingQueryRunner implements ReactiveQueryRunner {

		private final ReactiveQueryRunner delegate;

		private final Collection<Bookmark> usedBookmarks;

		private final BiConsumer<Collection<Bookmark>, Collection<Bookmark>> newBookmarkConsumer;

		private DelegatingQueryRunner(ReactiveQueryRunner delegate, Collection<Bookmark> lastBookmarks,
				BiConsumer<Collection<Bookmark>, Collection<Bookmark>> newBookmarkConsumer) {
			this.delegate = delegate;
			this.usedBookmarks = lastBookmarks;
			this.newBookmarkConsumer = newBookmarkConsumer;
		}

		Mono<Void> close() {

			// We're only going to close sessions we have acquired inside the client, not
			// something that
			// has been retrieved from the tx manager.
			if (this.delegate instanceof ReactiveSession session) {
				return Mono.fromDirect(session.close())
					.then()
					.doOnSuccess(
							signal -> this.newBookmarkConsumer.accept(this.usedBookmarks, session.lastBookmarks()));
			}

			return Mono.empty();
		}

		@Override
		public Publisher<ReactiveResult> run(String query, Value parameters) {
			return this.delegate.run(query, parameters);
		}

		@Override
		public Publisher<ReactiveResult> run(String query, Map<String, Object> parameters) {
			return this.delegate.run(query, parameters);
		}

		@Override
		public Publisher<ReactiveResult> run(String query, Record parameters) {
			return this.delegate.run(query, parameters);
		}

		@Override
		public Publisher<ReactiveResult> run(String query) {
			return this.delegate.run(query);
		}

		@Override
		public Publisher<ReactiveResult> run(Query query) {
			return this.delegate.run(query);
		}

	}

	class DefaultRunnableSpec implements UnboundRunnableSpec, RunnableSpecBoundToDatabaseAndUser {

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters = new NamedParameters();

		private Mono<DatabaseSelection> databaseSelection;

		private Mono<UserSelection> userSelection;

		DefaultRunnableSpec(Supplier<String> cypherSupplier) {
			this.databaseSelection = resolveTargetDatabaseName(null);
			this.userSelection = resolveUser(null);
			this.cypherSupplier = cypherSupplier;
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
		public <T> Neo4jClient.OngoingBindSpec<T, RunnableSpec> bind(@Nullable T value) {
			return new DefaultOngoingBindSpec<>(value);
		}

		@Override
		public RunnableSpec bindAll(Map<String, Object> newParameters) {
			this.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <R> MappingSpec<R> fetchAs(Class<R> targetClass) {

			return new DefaultRecordFetchSpec<>(this.databaseSelection, this.userSelection, this.cypherSupplier,
					this.parameters,
					new SingleValueMappingFunction<>(DefaultReactiveNeo4jClient.this.conversionService, targetClass));
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(this.databaseSelection, this.userSelection, this.cypherSupplier,
					this.parameters, (t, r) -> r.asMap());
		}

		@Override
		public Mono<ResultSummary> run() {

			return new DefaultRecordFetchSpec<>(this.databaseSelection, this.userSelection, this.cypherSupplier,
					this.parameters, (t, r) -> null)
				.run();
		}

		class DefaultOngoingBindSpec<T> implements Neo4jClient.OngoingBindSpec<T, RunnableSpec> {

			@Nullable
			private final T value;

			DefaultOngoingBindSpec(@Nullable T value) {
				this.value = value;
			}

			@Override
			public RunnableSpec to(String name) {

				DefaultRunnableSpec.this.parameters.add(name, this.value);
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
			public Mono<ResultSummary> run() {
				return DefaultRunnableSpec.this.run();
			}

			@Override
			public <T> Neo4jClient.OngoingBindSpec<T, RunnableSpec> bind(@Nullable T value) {
				return DefaultRunnableSpec.this.bind(value);
			}

			@Override
			public RunnableSpec bindAll(Map<String, Object> newParameters) {
				return DefaultRunnableSpec.this.bindAll(newParameters);
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
			public Mono<ResultSummary> run() {
				return DefaultRunnableSpec.this.run();
			}

			@Override
			public <T> Neo4jClient.OngoingBindSpec<T, RunnableSpec> bind(@Nullable T value) {
				return DefaultRunnableSpec.this.bind(value);
			}

			@Override
			public RunnableSpec bindAll(Map<String, Object> newParameters) {
				return DefaultRunnableSpec.this.bindAll(newParameters);
			}

		}

	}

	class DefaultRecordFetchSpec<T> implements RecordFetchSpec<T>, MappingSpec<T> {

		private final Mono<DatabaseSelection> databaseSelection;

		private final Mono<UserSelection> userSelection;

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		private BiFunction<TypeSystem, Record, T> mappingFunction;

		DefaultRecordFetchSpec(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection,
				Supplier<String> cypherSupplier, NamedParameters parameters,
				BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.databaseSelection = databaseSelection;
			this.userSelection = userSelection;
			this.cypherSupplier = cypherSupplier;
			this.parameters = parameters;
			this.mappingFunction = mappingFunction;
		}

		@Override
		public RecordFetchSpec<T> mappedBy(
				@SuppressWarnings("HiddenField") BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = mappingFunction;
			return this;
		}

		Mono<Tuple2<String, Map<String, Object>>> prepareStatement() {
			if (cypherLog.isDebugEnabled()) {
				String cypher = this.cypherSupplier.get();
				cypherLog.debug(() -> String.format("Executing:%s%s", System.lineSeparator(), cypher));

				if (cypherLog.isTraceEnabled() && !this.parameters.isEmpty()) {
					cypherLog
						.trace(() -> String.format("with parameters:%s%s", System.lineSeparator(), this.parameters));
				}
			}
			return Mono.fromSupplier(this.cypherSupplier).zipWith(Mono.just(this.parameters.get()));
		}

		Flux<T> executeWith(Tuple2<String, Map<String, Object>> t, ReactiveQueryRunner runner) {

			return Flux.usingWhen(Flux.from(runner.run(t.getT1(), t.getT2())),
					result -> Flux.from(result.records()).flatMap(r -> {
						if (this.mappingFunction instanceof SingleValueMappingFunction && r.size() == 1
								&& r.get(0).hasType(TypeSystem.getDefault().LIST())) {
							return Flux.fromStream(r.get(0)
								.asList(v -> ((SingleValueMappingFunction<T>) this.mappingFunction).convertValue(v))
								.stream());
						}
						var item = this.mappingFunction.apply(TypeSystem.getDefault(), r);
						return (item != null) ? Flux.just(item) : Flux.empty();
					}), result -> Flux.from(result.consume()).doOnNext(ResultSummaries::process));
		}

		@Override
		public Mono<T> one() {

			return doInQueryRunnerForMono(this.databaseSelection, this.userSelection,
					(runner) -> prepareStatement().flatMapMany(t -> executeWith(t, runner))
						.singleOrEmpty()
						.onErrorMap(RuntimeException.class,
								DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException));
		}

		@Override
		public Mono<T> first() {

			return doInQueryRunnerForMono(this.databaseSelection, this.userSelection,
					runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).next())
				.onErrorMap(RuntimeException.class,
						DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}

		@Override
		public Flux<T> all() {

			return doInStatementRunnerForFlux(this.databaseSelection, this.userSelection,
					runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)))
				.onErrorMap(RuntimeException.class,
						DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}

		Mono<ResultSummary> run() {

			return doInQueryRunnerForMono(this.databaseSelection, this.userSelection,
					runner -> prepareStatement().flatMap(t -> Flux.from(runner.run(t.getT1(), t.getT2())).single())
						.flatMap(rxResult -> Flux.from(rxResult.consume()).single().map(ResultSummaries::process)))
				.onErrorMap(RuntimeException.class,
						DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}

	}

	class DefaultRunnableDelegation<T> implements RunnableDelegation<T>, OngoingDelegation<T> {

		private final Function<ReactiveQueryRunner, Mono<T>> callback;

		private final Mono<UserSelection> userSelection;

		private Mono<DatabaseSelection> databaseSelection;

		DefaultRunnableDelegation(Function<ReactiveQueryRunner, Mono<T>> callback) {
			this.callback = callback;
			this.databaseSelection = resolveTargetDatabaseName(null);
			this.userSelection = resolveUser(null);
		}

		@Override
		public RunnableDelegation<T> in(@SuppressWarnings("HiddenField") String targetDatabase) {

			this.databaseSelection = resolveTargetDatabaseName(targetDatabase);
			return this;
		}

		@Override
		public Mono<T> run() {

			return doInQueryRunnerForMono(this.databaseSelection, this.userSelection, this.callback);
		}

	}

}
