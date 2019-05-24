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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.neo4j.driver.summary.ResultSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.StatementBuilder;
import org.springframework.data.neo4j.core.cypher.StatementBuilder.OngoingMatchAndReturn;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
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
class SimpleNeo4jRepository<T, ID> implements PagingAndSortingRepository<T, ID> {

	private static final Renderer renderer = CypherRenderer.create();

	private final Neo4jClient neo4jClient;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final NodeDescription<T> nodeDescription;

	private final SchemaBasedStatementBuilder statementBuilder;

	SimpleNeo4jRepository(Neo4jClient neo4jClient, Neo4jEntityInformation<T, ID> entityInformation,
		SchemaBasedStatementBuilder statementBuilder) {
		this.neo4jClient = neo4jClient;
		this.entityInformation = entityInformation;
		this.nodeDescription = this.entityInformation.getNodeDescription();
		this.statementBuilder = statementBuilder;
	}

	@Override
	public Iterable<T> findAll(Sort sort) {

		Statement statement = statementBuilder.prepareMatchOf(nodeDescription)
			.returning(asterisk())
			.orderBy(toSortItems(nodeDescription, sort))
			.build();

		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		OngoingMatchAndReturn returning = statementBuilder.prepareMatchOf(nodeDescription)
			.returning(asterisk());

		StatementBuilder.BuildableMatch returningWithPaging = addPagingParameter(nodeDescription, pageable, returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = createExecutableQuery(statement).getResults();
		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {
		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	@Transactional
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		throw new UnsupportedOperationException("Not there yet.");
	}

	@Override
	public Optional<T> findById(ID id) {

		Statement statement = statementBuilder
			.prepareMatchOf(nodeDescription,
				Optional.of(entityInformation.getIdExpression().isEqualTo(literalOf(id))))
			.returning(asterisk())
			.build();
		return createExecutableQuery(statement).getSingleResult();
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	public Iterable<T> findAll() {

		Statement statement = statementBuilder.prepareMatchOf(nodeDescription)
			.returning(asterisk()).build();
		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Statement statement = statementBuilder
			.prepareMatchOf(nodeDescription, Optional.of(entityInformation.getIdExpression().in((parameter("ids")))))
			.returning(asterisk())
			.build();

		return createExecutableQuery(statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public long count() {

		Statement statement = statementBuilder.prepareMatchOf(nodeDescription)
			.returning(Functions.count(asterisk())).build();

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.build();
		return ExecutableQuery.create(this.neo4jClient, preparedQuery)
			.getRequiredSingleResult();
	}

	@Override
	@Transactional
	public void deleteById(ID id) {

		String nameOfParameter = "id";
		Condition condition = this.entityInformation.getIdExpression().isEqualTo(parameter(nameOfParameter));

		log.debug("Deleting entity with id {} ", id);

		Statement statement = statementBuilder
			.prepareDeleteOf(nodeDescription, Optional.of(condition)).build();
		ResultSummary summary = this.neo4jClient.newQuery(renderer.render(statement))
			.bind(id).to(nameOfParameter)
			.run();

		log.debug("Deleted {} entities.", summary.counters().nodesDeleted());
	}

	@Override
	@Transactional
	public void delete(T entity) {

		ID id = (ID) this.entityInformation.getId(entity);
		this.deleteById(id);
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		String nameOfParameter = "ids";
		Condition condition = entityInformation.getIdExpression().in(parameter(nameOfParameter));

		List<Object> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInformation::getId).collect(Collectors.toList());

		log.debug("Deleting all entities with the following ids: {} ", ids);

		Statement statement = statementBuilder.prepareDeleteOf(nodeDescription, Optional.of(condition)).build();
		ResultSummary summary = this.neo4jClient.newQuery(renderer.render(statement))
			.bind(ids).to(nameOfParameter)
			.run();

		log.debug("Deleted {} entities.", summary.counters().nodesDeleted());
	}

	@Override
	@Transactional
	public void deleteAll() {

		log.debug("Deleting all nodes with primary label {}", nodeDescription.getPrimaryLabel());

		Statement statement = statementBuilder.prepareDeleteOf(nodeDescription, Optional.empty()).build();
		ResultSummary summary = this.neo4jClient.newQuery(renderer.render(statement)).run();

		log.debug("Deleted {} entities.", summary.counters().nodesDeleted());
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
		return ExecutableQuery.create(this.neo4jClient, preparedQuery);
	}
}
