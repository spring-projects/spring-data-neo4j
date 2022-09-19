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

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.reactive.RxQueryRunner;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reactive variant of the {@link Neo4jClient}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 6.0
 */
final class DefaultReactiveNeo4jClient implements ReactiveNeo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;
	private @Nullable final ReactiveDatabaseSelectionProvider databaseSelectionProvider;
	private @Nullable final ReactiveUserSelectionProvider userSelectionProvider;
	private final ConversionService conversionService;
	private final Neo4jPersistenceExceptionTranslator persistenceExceptionTranslator = new Neo4jPersistenceExceptionTranslator();

	// Basically a local bookmark manager
	private final Set<Bookmark> bookmarks = new HashSet<>();
	private final ReentrantReadWriteLock bookmarksLock = new ReentrantReadWriteLock();

	DefaultReactiveNeo4jClient(Builder builder) {

		this.driver = builder.driver;
		this.typeSystem = driver.defaultTypeSystem();
		this.databaseSelectionProvider = builder.databaseSelectionProvider;
		this.userSelectionProvider = builder.impersonatedUserProvider;

		this.conversionService = new DefaultConversionService();
		Optional.ofNullable(builder.neo4jConversions).orElseGet(Neo4jConversions::new).registerConvertersIn((ConverterRegistry) conversionService);
	}

	@Override
	public Mono<RxQueryRunner> getQueryRunner(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection) {

		return databaseSelection.zipWith(userSelection)
				.flatMap(targetDatabaseAndUser ->
				ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, targetDatabaseAndUser.getT1(), targetDatabaseAndUser.getT2())
						.map(RxQueryRunner.class::cast)
						.zipWith(Mono.just(Collections.<Bookmark>emptySet()))
						.switchIfEmpty(Mono.fromSupplier(() -> {
							ReentrantReadWriteLock.ReadLock lock = bookmarksLock.readLock();
							try {
								lock.lock();
								Set<Bookmark> lastBookmarks = new HashSet<>(bookmarks);
								return Tuples.of(driver.rxSession(Neo4jTransactionUtils.sessionConfig(false, lastBookmarks, targetDatabaseAndUser.getT1(), targetDatabaseAndUser.getT2())), lastBookmarks);
							} finally {
								lock.unlock();
							}
						})))
				.map(t -> new DelegatingQueryRunner(t.getT1(), t.getT2(), (usedBookmarks, newBookmark) -> {
					ReentrantReadWriteLock.WriteLock lock = bookmarksLock.writeLock();
					try {
						lock.lock();
						bookmarks.removeAll(usedBookmarks);
						bookmarks.add(newBookmark);
					} finally {
						lock.unlock();
					}
				}));
	}

	private static class DelegatingQueryRunner implements RxQueryRunner {

		private final RxQueryRunner delegate;
		private final Collection<Bookmark> usedBookmarks;
		private final BiConsumer<Collection<Bookmark>, Bookmark> newBookmarkConsumer;

		private DelegatingQueryRunner(RxQueryRunner delegate, Collection<Bookmark> lastBookmarks, BiConsumer<Collection<Bookmark>, Bookmark> newBookmarkConsumer) {
			this.delegate = delegate;
			this.usedBookmarks = lastBookmarks;
			this.newBookmarkConsumer = newBookmarkConsumer;
		}

		Mono<Void> close() {

			// We're only going to close sessions we have acquired inside the client, not something that
			// has been retrieved from the tx manager.
			if (this.delegate instanceof RxSession) {
				RxSession session = (RxSession) this.delegate;
				return Mono.fromDirect(session.close()).then().doOnSuccess(signal ->
						this.newBookmarkConsumer.accept(usedBookmarks, session.lastBookmark()));
			}

			return Mono.empty();
		}

		@Override
		public RxResult run(String query, Value parameters) {
			return delegate.run(query, parameters);
		}

		@Override
		public RxResult run(String query, Map<String, Object> parameters) {
			return delegate.run(query, parameters);
		}

		@Override
		public RxResult run(String query, Record parameters) {
			return delegate.run(query, parameters);
		}

		@Override
		public RxResult run(String query) {
			return delegate.run(query);
		}

		@Override
		public RxResult run(Query query) {
			return delegate.run(query);
		}
	}

	<T> Mono<T> doInQueryRunnerForMono(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection, Function<RxQueryRunner, Mono<T>> func) {

		return Mono.usingWhen(getQueryRunner(databaseSelection, userSelection), func, runner -> ((DelegatingQueryRunner) runner).close());
	}

	<T> Flux<T> doInStatementRunnerForFlux(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection, Function<RxQueryRunner, Flux<T>> func) {

		return Flux.usingWhen(getQueryRunner(databaseSelection, userSelection), func, runner -> ((DelegatingQueryRunner) runner).close());
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
	public <T> OngoingDelegation<T> delegateTo(Function<RxQueryRunner, Mono<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
	}

	@Override
	@Nullable
	public ReactiveDatabaseSelectionProvider getDatabaseSelectionProvider() {
		return databaseSelectionProvider;
	}

	private Mono<DatabaseSelection> resolveTargetDatabaseName(@Nullable String parameterTargetDatabase) {

		String value = Neo4jClient.verifyDatabaseName(parameterTargetDatabase);
		if (value != null) {
			return Mono.just(DatabaseSelection.byName(value));
		}
		if (databaseSelectionProvider != null) {
			return databaseSelectionProvider.getDatabaseSelection();
		}
		return ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider().getDatabaseSelection();
	}

	private Mono<UserSelection> resolveUser(@Nullable String userName) {

		if (StringUtils.hasText(userName)) {
			return Mono.just(UserSelection.impersonate(userName));
		}
		if (userSelectionProvider != null) {
			return userSelectionProvider.getUserSelection();
		}
		return ReactiveUserSelectionProvider.getDefaultSelectionProvider().getUserSelection();
	}

	class DefaultRunnableSpec implements UnboundRunnableSpec, RunnableSpecBoundToDatabaseAndUser {

		private final Supplier<String> cypherSupplier;

		private Mono<DatabaseSelection> databaseSelection;

		private Mono<UserSelection> userSelection;

		private final NamedParameters parameters = new NamedParameters();

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
		public <T> Neo4jClient.OngoingBindSpec<T, RunnableSpec> bind(T value) {
			return new DefaultOngoingBindSpec<>(value);
		}

		@Override
		public RunnableSpec bindAll(Map<String, Object> newParameters) {
			this.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <R> MappingSpec<R> fetchAs(Class<R> targetClass) {

			return new DefaultRecordFetchSpec<>(databaseSelection, userSelection, cypherSupplier, parameters,
					new SingleValueMappingFunction<>(conversionService, targetClass));
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(databaseSelection, userSelection, cypherSupplier, parameters, (t, r) -> r.asMap());
		}

		@Override
		public Mono<ResultSummary> run() {

			return new DefaultRecordFetchSpec<>(databaseSelection, userSelection, cypherSupplier, this.parameters, null).run();
		}

		class DefaultOngoingBindSpec<T> implements Neo4jClient.OngoingBindSpec<T, RunnableSpec> {

			@Nullable private final T value;

			DefaultOngoingBindSpec(@Nullable T value) {
				this.value = value;
			}

			@Override
			public RunnableSpec to(String name) {

				DefaultRunnableSpec.this.parameters.add(name, value);
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
			public Mono<ResultSummary> run() {
				return DefaultRunnableSpec.this.run();
			}

			@Override
			public <T> Neo4jClient.OngoingBindSpec<T, RunnableSpec> bind(T value) {
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
			public <T> Neo4jClient.OngoingBindSpec<T, RunnableSpec> bind(T value) {
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

		DefaultRecordFetchSpec(Mono<DatabaseSelection> databaseSelection, Mono<UserSelection> userSelection, Supplier<String> cypherSupplier, NamedParameters parameters,
				@Nullable BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.databaseSelection = databaseSelection;
			this.userSelection = userSelection;
			this.cypherSupplier = cypherSupplier;
			this.parameters = parameters;
			this.mappingFunction = mappingFunction;
		}

		@Override
		public RecordFetchSpec<T> mappedBy(@SuppressWarnings("HiddenField") BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = mappingFunction;
			return this;
		}

		Mono<Tuple2<String, Map<String, Object>>> prepareStatement() {
			if (cypherLog.isDebugEnabled()) {
				String cypher = cypherSupplier.get();
				cypherLog.debug(() -> String.format("Executing:%s%s", System.lineSeparator(), cypher));

				if (cypherLog.isTraceEnabled() && !parameters.isEmpty()) {
					cypherLog.trace(() -> String.format("with parameters:%s%s", System.lineSeparator(), parameters));
				}
			}
			return Mono.fromSupplier(cypherSupplier).zipWith(Mono.just(parameters.get()));
		}

		Flux<T> executeWith(Tuple2<String, Map<String, Object>> t, RxQueryRunner runner) {

			return Flux.usingWhen(Flux.just(runner.run(t.getT1(), t.getT2())),
					result -> Flux.from(result.records()).mapNotNull(r -> mappingFunction.apply(typeSystem, r)),
					result -> Flux.from(result.consume()).doOnNext(ResultSummaries::process));
		}

		@Override
		public Mono<T> one() {

			return doInQueryRunnerForMono(databaseSelection, userSelection,
					(runner) -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).singleOrEmpty())
							.onErrorMap(RuntimeException.class, DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}

		@Override
		public Mono<T> first() {

			return doInQueryRunnerForMono(databaseSelection, userSelection,
					runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).next())
							.onErrorMap(RuntimeException.class, DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}

		@Override
		public Flux<T> all() {

			return doInStatementRunnerForFlux(databaseSelection, userSelection,
					runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)))
					.onErrorMap(RuntimeException.class, DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}

		Mono<ResultSummary> run() {

			return doInQueryRunnerForMono(databaseSelection, userSelection, runner -> prepareStatement().flatMap(t -> {
				RxResult rxResult = runner.run(t.getT1(), t.getT2());
				return Flux.from(rxResult.records()).then(Mono.from(rxResult.consume()).map(ResultSummaries::process));
			})).onErrorMap(RuntimeException.class, DefaultReactiveNeo4jClient.this::potentiallyConvertRuntimeException);
		}
	}

	/**
	 * Tries to convert the given {@link RuntimeException} into a {@link DataAccessException} but returns the original
	 * exception if the conversation failed. Thus allows safe re-throwing of the return value.
	 *
	 * @param ex the exception to translate
	 * @return Any translated exception
	 */
	private RuntimeException potentiallyConvertRuntimeException(RuntimeException ex) {
		RuntimeException resolved = persistenceExceptionTranslator.translateExceptionIfPossible(ex);
		return resolved == null ? ex : resolved;
	}

	class DefaultRunnableDelegation<T> implements RunnableDelegation<T>, OngoingDelegation<T> {

		private final Function<RxQueryRunner, Mono<T>> callback;

		private Mono<DatabaseSelection> databaseSelection;
		private Mono<UserSelection> userSelection;

		DefaultRunnableDelegation(Function<RxQueryRunner, Mono<T>> callback) {
			this.callback = callback;
			this.databaseSelection = resolveTargetDatabaseName(null);
			this.userSelection = resolveUser(null);
		}

		@Override
		public RunnableDelegation<T> in(@Nullable @SuppressWarnings("HiddenField") String targetDatabase) {

			this.databaseSelection = resolveTargetDatabaseName(targetDatabase);
			return this;
		}

		@Override
		public Mono<T> run() {

			return doInQueryRunnerForMono(databaseSelection, userSelection, callback);
		}
	}
}
