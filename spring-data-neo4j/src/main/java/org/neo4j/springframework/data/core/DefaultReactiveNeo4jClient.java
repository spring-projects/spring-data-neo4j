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

import static org.neo4j.springframework.data.core.Neo4jClient.*;
import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionUtils.*;
import static org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxQueryRunner;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.Neo4jClient.*;
import org.neo4j.springframework.data.core.convert.Neo4jConversions;
import org.reactivestreams.Publisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
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
class DefaultReactiveNeo4jClient implements ReactiveNeo4jClient {

	private final Driver driver;
	private final TypeSystem typeSystem;
	private final ConversionService conversionService;

	DefaultReactiveNeo4jClient(Driver driver) {

		this.driver = driver;
		this.typeSystem = driver.defaultTypeSystem();

		this.conversionService = new DefaultConversionService();
		new Neo4jConversions().registerConvertersIn((ConverterRegistry) conversionService);
	}

	Mono<RxStatementRunnerHolder> retrieveRxStatementRunnerHolder(String targetDatabase) {

		return retrieveReactiveTransaction(driver, targetDatabase)
			.map(rxTransaction -> new RxStatementRunnerHolder(rxTransaction, Mono.empty(), Mono.empty())) //
			.switchIfEmpty(
				Mono.using(() -> driver.rxSession(defaultSessionConfig(targetDatabase)),
					session -> Mono.from(session.beginTransaction())
						.map(tx -> new RxStatementRunnerHolder(tx, tx.commit(), tx.rollback())), RxSession::close)
			);
	}

	<T> Mono<T> doInQueryRunnerForMono(final String targetDatabase, Function<RxQueryRunner, Mono<T>> func) {

		return Mono.usingWhen(retrieveRxStatementRunnerHolder(targetDatabase),
			holder -> func.apply(holder.getRxQueryRunner()),
			RxStatementRunnerHolder::getCommit,
			(holder, ex) -> holder.getRollback(),
			RxStatementRunnerHolder::getCommit);
	}

	<T> Flux<T> doInStatementRunnerForFlux(final String targetDatabase, Function<RxQueryRunner, Flux<T>> func) {

		return Flux.usingWhen(retrieveRxStatementRunnerHolder(targetDatabase),
			holder -> func.apply(holder.getRxQueryRunner()),
			RxStatementRunnerHolder::getCommit,
			(holder, ex) -> holder.getRollback(),
			RxStatementRunnerHolder::getCommit);
	}

	@Override
	public RunnableSpec query(String cypher) {
		return query(() -> cypher);
	}

	@Override
	public RunnableSpec query(Supplier<String> cypherSupplier) {
		return new DefaultRunnableSpec(cypherSupplier);
	}

	@Override
	public <T> OngoingDelegation<T> delegateTo(Function<RxQueryRunner, Mono<T>> callback) {
		return new DefaultRunnableDelegation<>(callback);
	}

	class DefaultRunnableSpec implements RunnableSpec {

		private final Supplier<String> cypherSupplier;

		private String targetDatabase;

		private final NamedParameters parameters = new NamedParameters();

		DefaultRunnableSpec(Supplier<String> cypherSupplier) {
			this.cypherSupplier = cypherSupplier;
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

				DefaultRunnableSpec.this.parameters.add(name, value);
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
			this.parameters.addAll(newParameters);
			return this;
		}

		@Override
		public <R> MappingSpec<R> fetchAs(Class<R> targetClass) {

			return new DefaultRecordFetchSpec<>(this.targetDatabase, this.cypherSupplier, this.parameters,
				new SingleValueMappingFunction(conversionService, targetClass));
		}

		@Override
		public RecordFetchSpec<Map<String, Object>> fetch() {

			return new DefaultRecordFetchSpec<>(targetDatabase, cypherSupplier, parameters,
				(t, r) -> r.asMap());
		}

		@Override
		public Mono<ResultSummary> run() {

			return new DefaultRecordFetchSpec<>(
				this.targetDatabase,
				this.cypherSupplier,
				this.parameters).run();
		}
	}

	class DefaultRecordFetchSpec<T> implements RecordFetchSpec<T>, MappingSpec<T> {

		private final String targetDatabase;

		private final Supplier<String> cypherSupplier;

		private final NamedParameters parameters;

		private BiFunction<TypeSystem, Record, T> mappingFunction;

		DefaultRecordFetchSpec(String targetDatabase, Supplier<String> cypherSupplier,
			NamedParameters parameters) {
			this(targetDatabase, cypherSupplier, parameters, null);
		}

		DefaultRecordFetchSpec(
			String targetDatabase, Supplier<String> cypherSupplier, NamedParameters parameters,
			@Nullable BiFunction<TypeSystem, Record, T> mappingFunction) {
			this.targetDatabase = targetDatabase;
			this.cypherSupplier = cypherSupplier;
			this.parameters = parameters;
			this.mappingFunction = mappingFunction;
		}

		@Override
		public RecordFetchSpec<T> mappedBy(BiFunction<TypeSystem, Record, T> mappingFunction) {

			this.mappingFunction = new DelegatingMappingFunctionWithNullCheck<>(mappingFunction);
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

			return Flux.from(runner.run(t.getT1(), t.getT2()).records()).map(r -> mappingFunction.apply(typeSystem, r));
		}

		@Override
		public Mono<T> one() {

			return doInQueryRunnerForMono(
				targetDatabase,
				(runner) -> prepareStatement().flatMapMany(t -> executeWith(t, runner)).singleOrEmpty());
		}

		@Override
		public Mono<T> first() {

			return doInQueryRunnerForMono(
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

			return doInQueryRunnerForMono(
				targetDatabase,
				runner -> prepareStatement().flatMap(t -> Mono.from(runner.run(t.getT1(), t.getT2()).consume())));
		}
	}

	class DefaultRunnableDelegation<T> implements RunnableDelegation<T>, OngoingDelegation<T> {

		private final Function<RxQueryRunner, Mono<T>> callback;

		private String targetDatabase;

		DefaultRunnableDelegation(Function<RxQueryRunner, Mono<T>> callback) {
			this(callback, null);
		}

		DefaultRunnableDelegation(Function<RxQueryRunner, Mono<T>> callback,
			@Nullable String targetDatabase) {
			this.callback = callback;
			this.targetDatabase = targetDatabase;
		}

		@Override
		public RunnableDelegation in(@Nullable @SuppressWarnings("HiddenField") String targetDatabase) {

			this.targetDatabase = verifyDatabaseName(targetDatabase);
			return this;
		}

		@Override
		public Mono<T> run() {

			return doInQueryRunnerForMono(
				targetDatabase,
				callback
			);

		}
	}

	final class RxStatementRunnerHolder {
		private final RxQueryRunner rxQueryRunner;

		private final Publisher<Void> commit;
		private final Publisher<Void> rollback;

		RxStatementRunnerHolder(RxQueryRunner rxQueryRunner, Publisher<Void> commit, Publisher<Void> rollback) {
			this.rxQueryRunner = rxQueryRunner;
			this.commit = commit;
			this.rollback = rollback;
		}

		public RxQueryRunner getRxQueryRunner() {
			return rxQueryRunner;
		}

		public Publisher<Void> getCommit() {
			return commit;
		}

		public Publisher<Void> getRollback() {
			return rollback;
		}
	}

}
