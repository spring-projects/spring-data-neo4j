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
package org.springframework.data.neo4j.repository.support;

import static java.util.Collections.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;
import static org.springframework.data.neo4j.repository.query.CypherAdapterUtils.*;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient.ExecutableQuery;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository base implementation for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@Repository
@Transactional(readOnly = true)
@Slf4j
class SimpleReactiveNeo4jRepository<T, ID> implements ReactiveSortingRepository<T, ID> {

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final NodeDescription<T> entityMetaData;

	private final SchemaBasedStatementBuilder statementBuilder;

	SimpleReactiveNeo4jRepository(ReactiveNeo4jClient neo4jClient, Neo4jEntityInformation<T, ID> entityInformation,
		SchemaBasedStatementBuilder statementBuilder) {
		this.neo4jClient = neo4jClient;
		this.entityInformation = entityInformation;
		this.entityMetaData = this.entityInformation.getEntityMetaData();
		this.statementBuilder = statementBuilder;
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

	@Override public <S extends T> Mono<S> save(S entity) {
		return null;
	}

	@Override public <S extends T> Flux<S> saveAll(Iterable<S> entities) {
		return null;
	}

	@Override public <S extends T> Flux<S> saveAll(Publisher<S> entityStream) {
		return null;
	}

	@Override public Mono<Void> deleteById(ID id) {
		return null;
	}

	@Override public Mono<Void> deleteById(Publisher<ID> id) {
		return null;
	}

	@Override public Mono<Void> delete(T entity) {
		return null;
	}

	@Override public Mono<Void> deleteAll(Iterable<? extends T> entities) {
		return null;
	}

	@Override public Mono<Void> deleteAll(Publisher<? extends T> entityStream) {
		return null;
	}

	@Override public Mono<Void> deleteAll() {
		return null;
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
