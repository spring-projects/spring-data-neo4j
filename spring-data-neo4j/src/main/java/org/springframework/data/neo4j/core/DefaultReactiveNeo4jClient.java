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

import static org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils.*;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxStatementRunner;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.reactivestreams.Publisher;
import org.springframework.data.neo4j.core.Neo4jClient.MappingSpec;
import org.springframework.data.neo4j.core.Neo4jClient.OngoingBindSpec;
import org.springframework.data.neo4j.core.Neo4jClient.RecordFetchSpec;

/**
 * Reactive variant of the {@link Neo4jClient}.
 *
 * TODO Reactive transaction management is pretty obvious still open
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 1.0
 */
@Slf4j
class DefaultReactiveNeo4jClient implements ReactiveNeo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;

	DefaultReactiveNeo4jClient(Driver driver) {

		this.driver = driver;
		// This will go away
		try (Session session = this.driver.session(t -> t.withDefaultAccessMode(AccessMode.READ))) {
			typeSystem = session.typeSystem();
		}
	}

	// Internal helper methods for managing transactional state

	Mono<RxStatementRunner> getStatementRunner(final String targetDatabase) {
		return retrieveReactiveTransaction(driver, targetDatabase)
			.switchIfEmpty(
				Mono.using(() -> driver.rxSession(defaultSessionParameters(targetDatabase)),
					session -> Mono.from(session.beginTransaction()), RxSession::close)
			)
			.map(RxStatementRunner.class::cast);
	}

	static boolean isSpringTransactionManagementActive() {
		// TODO Something along the lines of if TransactionSynchronizationManager#isSynchronizationActive but for reactive
		return false;
	}

	static Publisher<Void> asyncComplete(RxStatementRunner runner) {

		if (runner instanceof RxTransaction && !isSpringTransactionManagementActive()) {
			return ((RxTransaction) runner).commit();
		}

		return Mono.empty();
	}

	static Publisher<Void> asyncCancel(RxStatementRunner runner) {

		log.debug("Request has been cancelled, ongoing transactions will be committed.");

		return asyncComplete(runner);
	}

	static Publisher<Void> asyncError(RxStatementRunner runner) {

		if (runner instanceof RxTransaction && !isSpringTransactionManagementActive()) {
			return ((RxTransaction) runner).rollback();
		}

		return Mono.empty();
	}

	// Below are all the implementations (methods and classes) as defined by the contracts of ReactiveNeo4jClient

	@Override
	public ReactiveRunnableSpec newQuery(String cypher) {
		return newQuery(() -> cypher);
	}

	@Override
	public ReactiveRunnableSpec newQuery(Supplier<String> cypherSupplier) {
		return new DefaultReactiveRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingReactiveDelegation<T> delegateTo(Function<RxStatementRunner, Mono<T>> callback) {
		return new DefaultReactiveRunnableDelegation<>(callback);
	}

	@RequiredArgsConstructor
	class DefaultReactiveRunnableSpec implements ReactiveRunnableSpec {

		private final Supplier<String> cypherSupplier;

		private String targetDatabase;

		private final NamedParameters parameters = new NamedParameters();

		@Override
		public ReactiveRunnableSpecTightToDatabase in(@SuppressWarnings("HiddenField") String targetDatabase) {
			this.targetDatabase = targetDatabase;
			return this;
		}

		@RequiredArgsConstructor
		class DefaultOngoingBindSpec<T> implements OngoingBindSpec<T, ReactiveRunnableSpecTightToDatabase> {

			private final T value;

			@Override
			public ReactiveRunnableSpecTightToDatabase to(String name) {

				DefaultReactiveRunnableSpec.this.parameters.add(name, value);
				return DefaultReactiveRunnableSpec.this;
			}

			@Override
			public ReactiveRunnableSpecTightToDatabase with(Function<T, Map<String, Object>> binder) {
				return bindAll(binder.apply(value));
			}
		}

		@Override
		public OngoingBindSpec<?, ReactiveRunnableSpecTightToDatabase> bind(Object value) {
			return new DefaultOngoingBindSpec(value);
		}

		@Override
		public ReactiveRunnableSpecTightToDatabase bindAll(Map<String, Object> newParameters) {
			this.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <R> MappingSpec<Mono<R>, Flux<R>, R> fetchAs(Class<R> targetClass) {

			return new DefaultReactiveRecordFetchSpec<>(this.targetDatabase, this.cypherSupplier, this.parameters,
				new SingleValueMappingFunction(targetClass));
		}

		@Override
		public RecordFetchSpec<Mono<Map<String, Object>>, Flux<Map<String, Object>>, Map<String, Object>> fetch() {

			return new DefaultReactiveRecordFetchSpec<>(targetDatabase, cypherSupplier, parameters,
				(t, r) -> r.asMap());
		}

		@Override
		public Mono<ResultSummary> run() {

			return new DefaultReactiveRecordFetchSpec<>(
				this.targetDatabase,
				this.cypherSupplier,
				this.parameters).run();
		}
	}

	@AllArgsConstructor
	@RequiredArgsConstructor
	class DefaultReactiveRecordFetchSpec<T>
		implements RecordFetchSpec<Mono<T>, Flux<T>, T>, MappingSpec<Mono<T>, Flux<T>, T> {

		private final String targetDatabase;

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		private BiFunction<TypeSystem, Record, T> mappingFunction;

		@Override
		public RecordFetchSpec<Mono<T>, Flux<T>, T> mappedBy(BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = new DelegatingMappingFunctionWithNullCheck<>(mappingFunction);
			return this;
		}

		Mono<Tuple2<String, Map<String, Object>>> prepareStatement() {

			return Mono.fromSupplier(cypherSupplier).zipWith(Mono.just(parameters.get()));
		}

		Flux<T> executeWith(Tuple2<String, Map<String, Object>> t, RxStatementRunner runner) {

			return Flux.from(runner.run(t.getT1(), t.getT2()).records()).map(r -> mappingFunction.apply(typeSystem, r));
		}

		@Override
		public Mono<T> one() {

			return Mono.usingWhen(
				getStatementRunner(targetDatabase),
				runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).single(),
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError,
				DefaultReactiveNeo4jClient::asyncCancel
			);
		}

		@Override
		public Mono<T> first() {

			return Mono.usingWhen(
				getStatementRunner(targetDatabase),
				runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).next(),
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError,
				DefaultReactiveNeo4jClient::asyncCancel
			);
		}

		@Override
		public Flux<T> all() {

			return Flux.usingWhen(
				getStatementRunner(targetDatabase),
				runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)),
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError,
				DefaultReactiveNeo4jClient::asyncCancel
			);
		}

		Mono<ResultSummary> run() {

			return Mono
				.usingWhen(
					getStatementRunner(targetDatabase),
					runner -> prepareStatement().flatMap(t -> Mono.from(runner.run(t.getT1(), t.getT2()).summary())),
					DefaultReactiveNeo4jClient::asyncComplete,
					DefaultReactiveNeo4jClient::asyncError,
					DefaultReactiveNeo4jClient::asyncCancel
				);
		}
	}

	@AllArgsConstructor
	@RequiredArgsConstructor
	class DefaultReactiveRunnableDelegation<T> implements ReactiveRunnableDelegation<T>, OngoingReactiveDelegation<T> {

		private final Function<RxStatementRunner, Mono<T>> callback;

		private String targetDatabase;

		@Override
		public ReactiveRunnableDelegation in(@SuppressWarnings("HiddenField") String targetDatabase) {
			this.targetDatabase = targetDatabase;
			return this;
		}

		@Override
		public Mono<T> run() {

			return Mono.usingWhen(
				getStatementRunner(targetDatabase),
				callback::apply,
				DefaultReactiveNeo4jClient::asyncComplete,
				DefaultReactiveNeo4jClient::asyncError,
				DefaultReactiveNeo4jClient::asyncCancel);
		}
	}
}
