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
import static org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager.*;

import lombok.AccessLevel;
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

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxStatementRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.reactivestreams.Publisher;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.core.Neo4jClient.MappingSpec;
import org.springframework.data.neo4j.core.Neo4jClient.OngoingBindSpec;
import org.springframework.data.neo4j.core.Neo4jClient.RecordFetchSpec;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Reactive variant of the {@link Neo4jClient}.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @soundtrack Die Toten Hosen - Im Auftrag des Herrn
 * @since 1.0
 */
@Slf4j
class DefaultReactiveNeo4jClient implements ReactiveNeo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;

	DefaultReactiveNeo4jClient(Driver driver) {

		this.driver = driver;
		this.typeSystem = driver.defaultTypeSystem();
	}

	Mono<RxStatementRunnerHolder> retrieveRxStatementRunnerHolder(String targetDatabase) {

		return retrieveReactiveTransaction(driver, targetDatabase)
			.map(rxTransaction -> new RxStatementRunnerHolder(rxTransaction, Mono.empty(), Mono.empty())) //
			.switchIfEmpty(
				Mono.using(() -> driver.rxSession(defaultSessionParameters(targetDatabase)),
					session -> Mono.from(session.beginTransaction())
						.map(tx -> new RxStatementRunnerHolder(tx, tx.commit(), tx.rollback())), RxSession::close)
			);
	}

	<T> Mono<T> doInStatementRunnerForMono(final String targetDatabase, Function<RxStatementRunner, Mono<T>> func) {

		return Mono.usingWhen(retrieveRxStatementRunnerHolder(targetDatabase),
			holder -> func.apply(holder.getRxStatementRunner()),
			RxStatementRunnerHolder::getCommit,
			RxStatementRunnerHolder::getRollback);

	}

	<T> Flux<T> doInStatementRunnerForFlux(final String targetDatabase, Function<RxStatementRunner, Flux<T>> func) {

		return Flux.usingWhen(retrieveRxStatementRunnerHolder(targetDatabase),
			holder -> func.apply(holder.getRxStatementRunner()),
			RxStatementRunnerHolder::getCommit,
			RxStatementRunnerHolder::getRollback);
	}

	@Override
	public ReactiveRunnableSpec query(String cypher) {
		return query(() -> cypher);
	}

	@Override
	public ReactiveRunnableSpec query(Supplier<String> cypherSupplier) {
		return new DefaultReactiveRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingReactiveDelegation<T> delegateTo(Function<RxStatementRunner, Mono<T>> callback) {
		return new DefaultReactiveRunnableDelegation<>(callback);
	}

	@Override
	public <T> ExecutableQuery<T> toExecutableQuery(PreparedQuery<T> preparedQuery) {

		Class<T> resultType = preparedQuery.getResultType();
		Neo4jClient.MappingSpec<Mono<T>, Flux<T>, T> mappingSpec = this
			.query(preparedQuery.getCypherQuery())
			.bindAll(preparedQuery.getParameters())
			.fetchAs(resultType);

		Neo4jClient.RecordFetchSpec<Mono<T>, Flux<T>, T> fetchSpec = preparedQuery
			.getOptionalMappingFunction()
			.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction))
			.orElse(mappingSpec);

		return new DefaultReactiveExecutableQuery<>(fetchSpec);
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

			@Nullable
			private final T value;

			@Override
			public ReactiveRunnableSpecTightToDatabase to(String name) {

				DefaultReactiveRunnableSpec.this.parameters.add(name, value);
				return DefaultReactiveRunnableSpec.this;
			}

			@Override
			public ReactiveRunnableSpecTightToDatabase with(Function<T, Map<String, Object>> binder) {

				Assert.notNull(binder, "Binder is required.");

				return bindAll(binder.apply(value));
			}
		}

		@Override
		public OngoingBindSpec<?, ReactiveRunnableSpecTightToDatabase> bind(@Nullable Object value) {
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
			if (cypherLog.isDebugEnabled()) {
				String cypher = cypherSupplier.get();
				cypherLog.debug("Executing:{}{}", System.lineSeparator(), cypher);

				if (cypherLog.isTraceEnabled() && !parameters.isEmpty()) {
					cypherLog.trace("with parameters:{}{}", System.lineSeparator(), parameters);
				}
			}
			return Mono.fromSupplier(cypherSupplier).zipWith(Mono.just(parameters.get()));
		}

		Flux<T> executeWith(Tuple2<String, Map<String, Object>> t, RxStatementRunner runner) {

			return Flux.from(runner.run(t.getT1(), t.getT2()).records()).map(r -> mappingFunction.apply(typeSystem, r));
		}

		@Override
		public Mono<T> one() {

			return doInStatementRunnerForMono(
				targetDatabase,
				(runner) -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).singleOrEmpty());
		}

		@Override
		public Mono<T> first() {

			return doInStatementRunnerForMono(
				targetDatabase,
				runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).next());
		}

		@Override
		public Flux<T> all() {

			return doInStatementRunnerForFlux(
				targetDatabase,
				runner -> prepareStatement().flatMapMany(t -> executeWith(t, runner))
			);
		}

		Mono<ResultSummary> run() {

			return doInStatementRunnerForMono(
				targetDatabase,
				runner -> prepareStatement().flatMap(t -> Mono.from(runner.run(t.getT1(), t.getT2()).summary())));
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

			return doInStatementRunnerForMono(
				targetDatabase,
				callback
			);

		}
	}

	@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
	final class DefaultReactiveExecutableQuery<T> implements ExecutableQuery<T> {

		private final Neo4jClient.RecordFetchSpec<Mono<T>, Flux<T>, T> fetchSpec;

		/**
		 * @return All results returned by this query.
		 */
		public Flux<T> getResults() {
			return fetchSpec.all();
		}

		/**
		 * @return A single result
		 * @throws IncorrectResultSizeDataAccessException
		 */
		public Mono<T> getSingleResult() {
			try {
				return fetchSpec.one();
			} catch (NoSuchRecordException e) {
				// This exception is thrown by the driver in both cases when there are 0 or 1+n records
				// So there has been an incorrect result size, but not to few results but to many.
				throw new IncorrectResultSizeDataAccessException(1);
			}
		}
	}

	final class RxStatementRunnerHolder {
		private final RxStatementRunner rxStatementRunner;

		private final Publisher<Void> commit;
		private final Publisher<Void> rollback;

		RxStatementRunnerHolder(RxStatementRunner rxStatementRunner, Publisher<Void> commit, Publisher<Void> rollback) {
			this.rxStatementRunner = rxStatementRunner;
			this.commit = commit;
			this.rollback = rollback;
		}

		public RxStatementRunner getRxStatementRunner() {
			return rxStatementRunner;
		}

		public Publisher<Void> getCommit() {
			return commit;
		}

		public Publisher<Void> getRollback() {
			return rollback;
		}
	}

}
