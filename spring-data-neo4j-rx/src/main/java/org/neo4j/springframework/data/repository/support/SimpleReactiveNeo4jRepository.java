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
package org.neo4j.springframework.data.repository.support;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;
import static org.neo4j.springframework.data.repository.query.CypherAdapterUtils.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient.ExecutableQuery;
import org.neo4j.springframework.data.core.cypher.Condition;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Repository base implementation for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@Repository
@Transactional(readOnly = true)
class SimpleReactiveNeo4jRepository<T, ID> implements ReactiveSortingRepository<T, ID> {

	private static final Logger log = LoggerFactory.getLogger(SimpleReactiveNeo4jRepository.class);

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final Neo4jPersistentEntity<T> entityMetaData;

	private final SchemaBasedStatementBuilder statementBuilder;

	private final ReactiveNeo4jEvents eventSupport;

	SimpleReactiveNeo4jRepository(ReactiveNeo4jClient neo4jClient, Neo4jEntityInformation<T, ID> entityInformation,
		SchemaBasedStatementBuilder statementBuilder,
		ReactiveNeo4jEvents eventSupport) {
		this.neo4jClient = neo4jClient;
		this.entityInformation = entityInformation;
		this.entityMetaData = this.entityInformation.getEntityMetaData();
		this.statementBuilder = statementBuilder;
		this.eventSupport = eventSupport;
	}

	@Override
	public Mono<T> findById(ID id) {
		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, Optional.of(entityInformation.getIdExpression().isEqualTo(literalOf(id))))
			.returning(asterisk())
			.build();
		return createExecutableQuery(statement).getSingleResult();
	}

	@Override
	public Mono<T> findById(Publisher<ID> idPublisher) {
		return Mono.from(idPublisher).flatMap(this::findById);
	}

	@Override
	public Flux<T> findAll(Sort sort) {
		Statement statement = statementBuilder.prepareMatchOf(entityMetaData, Optional.empty())
			.returning(asterisk())
			.orderBy(toSortItems(entityMetaData, sort))
			.build();

		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Mono<Boolean> existsById(ID id) {
		return findById(id).hasElement();
	}

	@Override
	public Mono<Boolean> existsById(Publisher<ID> idPublisher) {
		return Mono.from(idPublisher).flatMap(this::existsById);
	}

	@Override
	public Flux<T> findAll() {
		Statement statement = statementBuilder.prepareMatchOf(entityMetaData, Optional.empty())
			.returning(asterisk()).build();
		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Flux<T> findAllById(Iterable<ID> ids) {
		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, Optional.of(entityInformation.getIdExpression().in((parameter("ids")))))
			.returning(asterisk())
			.build();

		return createExecutableQuery(statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public Flux<T> findAllById(Publisher<ID> idStream) {
		return Flux.from(idStream).buffer().flatMap(this::findAllById);
	}

	@Override
	public Mono<Long> count() {
		Statement statement = statementBuilder.prepareMatchOf(entityMetaData, Optional.empty())
			.returning(Functions.count(asterisk())).build();

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.build();
		return this.neo4jClient.toExecutableQuery(preparedQuery)
			.getSingleResult();
	}

	@Override
	@Transactional
	public <S extends T> Mono<S> save(S entity) {

		return Mono.just(entity)
			.flatMap(eventSupport::maybeCallBeforeBind)
			.flatMap(it -> {
				Statement saveStatement = statementBuilder.prepareSaveOf(entityMetaData);

				Mono<Long> idMono =
					this.neo4jClient.query(() -> renderer.render(saveStatement))
						.bind((T) it).with(entityInformation.getBinderFunction())
						.fetchAs(Long.class).one();

				if (!entityMetaData.isUsingInternalIds()) {
					return idMono.thenReturn(it);
				} else {
					return idMono.map(internalId -> {
						PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(it);
						propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);
						return (S) propertyAccessor.getBean();
					});
				}
			});
	}

	@Override
	@Transactional
	public <S extends T> Flux<S> saveAll(Iterable<S> entities) {

		if (entityMetaData.isUsingInternalIds()) {
			log.debug("Saving entities using single statements.");

			return Flux.fromIterable(entities).flatMap(this::save);
		}

		final Function<T, Map<String, Object>> binderFunction = entityInformation.getBinderFunction();
		return Flux.fromIterable(entities)
			.flatMap(eventSupport::maybeCallBeforeBind)
			.collectList()
			.flatMapMany(
				entitiesToBeSaved -> Mono
					.defer(() -> { // Defer the actual save statement until the previous flux completes
						List<Map<String, Object>> boundedEntityList = entitiesToBeSaved.stream()
							.map(binderFunction)
							.collect(toList());

						return neo4jClient
							.query(() -> renderer
								.render(statementBuilder.prepareSaveOfMultipleInstancesOf(entityMetaData)))
							.bind(boundedEntityList).to(NAME_OF_ENTITY_LIST_PARAM).run();
					})
					.doOnNext(resultSummary -> {
						SummaryCounters counters = resultSummary.counters();
						log.debug(
							"Created {} and deleted {} nodes, created {} and deleted {} relationships and set {} properties.",
							counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
							counters.relationshipsDeleted(), counters.propertiesSet());
					})
					.thenMany(Flux.fromIterable(entitiesToBeSaved))
			);
	}

	@Override
	@Transactional
	public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {

		return Flux.from(entityStream).flatMap(this::save);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteById(java.lang.Object)
	 */
	@Override
	@Transactional
	public Mono<Void> deleteById(ID id) {

		Assert.notNull(id, "The given id must not be null!");

		String nameOfParameter = "id";
		Condition condition = this.entityInformation.getIdExpression().isEqualTo(parameter(nameOfParameter));

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, Optional.of(condition));
		return this.neo4jClient.query(() -> renderer.render(statement))
			.bind(id).to(nameOfParameter).run().then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#delete(java.lang.Object)
	 */
	@Override
	@Transactional
	public Mono<Void> delete(T entity) {

		Assert.notNull(entity, "The given entity must not be null!");
		return deleteById(this.entityInformation.getId(entity));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteById(org.reactivestreams.Publisher)
	 */
	@Override
	@Transactional
	public Mono<Void> deleteById(Publisher<ID> idPublisher) {

		Assert.notNull(idPublisher, "The given Publisher of an id must not be null!");
		return Mono.from(idPublisher).flatMap(this::deleteById);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteAll(java.lang.Iterable)
	 */
	@Override
	@Transactional
	public Mono<Void> deleteAll(Iterable<? extends T> entities) {

		Assert.notNull(entities, "The given Iterable of entities must not be null!");

		String nameOfParameter = "ids";
		Condition condition = entityInformation.getIdExpression().in(parameter(nameOfParameter));
		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, Optional.of(condition));

		List<ID> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInformation::getId)
			.collect(toList());

		return this.neo4jClient.query(() -> renderer.render(statement)).bind(ids).to(nameOfParameter).run().then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteAll(org.reactivestreams.Publisher)
	 */
	@Override
	@Transactional
	public Mono<Void> deleteAll(Publisher<? extends T> entitiesPublisher) {

		Assert.notNull(entitiesPublisher, "The given Publisher of entities must not be null!");
		return Flux.from(entitiesPublisher).flatMap(this::delete).then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.reactive.ReactiveCrudRepository#deleteAll()
	 */
	@Override
	@Transactional
	public Mono<Void> deleteAll() {

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, Optional.empty());
		return this.neo4jClient.query(() -> renderer.render(statement)).run().then();
	}

	private ExecutableQuery<T> createExecutableQuery(Statement statement) {
		return createExecutableQuery(statement, Collections.emptyMap());
	}

	private ExecutableQuery<T> createExecutableQuery(Statement statement, Map<String, Object> parameters) {

		PreparedQuery<T> preparedQuery = PreparedQuery.queryFor(this.entityInformation.getJavaType())
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(this.entityInformation.getMappingFunction())
			.build();
		return neo4jClient.toExecutableQuery(preparedQuery);
	}
}
