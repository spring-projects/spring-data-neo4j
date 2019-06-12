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

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.stream.StreamSupport;

import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.Neo4jClient.ExecutableQuery;
import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.cypher.Condition;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.StatementBuilder;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyAccessor;
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

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final Neo4jClient neo4jClient;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final Neo4jPersistentEntity<T> entityMetaData;

	private final SchemaBasedStatementBuilder statementBuilder;

	SimpleNeo4jRepository(Neo4jClient neo4jClient, Neo4jEntityInformation<T, ID> entityInformation,
		SchemaBasedStatementBuilder statementBuilder) {
		this.neo4jClient = neo4jClient;
		this.entityInformation = entityInformation;
		this.entityMetaData = this.entityInformation.getEntityMetaData();
		this.statementBuilder = statementBuilder;
	}

	@Override
	public Iterable<T> findAll(Sort sort) {

		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(asterisk())
			.orderBy(toSortItems(entityMetaData, sort))
			.build();

		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		OngoingReadingAndReturn returning = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(asterisk());

		StatementBuilder.BuildableStatement returningWithPaging = addPagingParameter(entityMetaData, pageable,
			returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = createExecutableQuery(statement).getResults();
		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {

		Long internalId = neo4jClient
			.query(() -> renderer.render(statementBuilder.prepareSaveOf(entityMetaData)))
			.bind((T) entity)
			.with(entityInformation.getBinderFunction())
			.fetchAs(Long.class).one().get();

		if (!entityMetaData.isUsingInternalIds()) {
			return entity;
		} else {
			PersistentPropertyAccessor<T> propertyAccessor = entityMetaData.getPropertyAccessor(entity);
			propertyAccessor.setProperty(entityMetaData.getRequiredIdProperty(), internalId);
			return (S) propertyAccessor.getBean();
		}
	}

	@Override
	@Transactional
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		if (entityMetaData.isUsingInternalIds()) {
			log.debug("Saving entities using single statements.");
			return StreamSupport.stream(entities.spliterator(), false)
				.map(this::save)
				.collect(toList());
		}

		final Function<T, Map<String, Object>> binderFunction = entityInformation.getBinderFunction();
		List<Map<String, Object>> entityList = StreamSupport.stream(entities.spliterator(), false)
			.map(binderFunction).collect(toList());

		ResultSummary resultSummary = neo4jClient
			.query(() -> renderer.render(statementBuilder.prepareSaveOfMultipleInstancesOf(entityMetaData)))
			.bind(entityList).to(NAME_OF_ENTITY_LIST_PARAM)
			.run();

		SummaryCounters counters = resultSummary.counters();
		log.debug("Created {} and deleted {} nodes, created {} and deleted {} relationships and set {} properties.",
			counters.nodesCreated(), counters.nodesDeleted(), counters.relationshipsCreated(),
			counters.relationshipsDeleted(), counters.propertiesSet());

		return entities;
	}

	@Override
	public Optional<T> findById(ID id) {

		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData,
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

		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(asterisk()).build();
		return createExecutableQuery(statement).getResults();
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Statement statement = statementBuilder
			.prepareMatchOf(entityMetaData, Optional.of(entityInformation.getIdExpression().in((parameter("ids")))))
			.returning(asterisk())
			.build();

		return createExecutableQuery(statement, singletonMap("ids", ids)).getResults();
	}

	@Override
	public long count() {

		Statement statement = statementBuilder.prepareMatchOf(entityMetaData)
			.returning(Functions.count(asterisk())).build();

		PreparedQuery<Long> preparedQuery = PreparedQuery.queryFor(Long.class)
			.withCypherQuery(renderer.render(statement))
			.build();
		return neo4jClient.toExecutableQuery(preparedQuery)
			.getRequiredSingleResult();
	}

	@Override
	@Transactional
	public void deleteById(ID id) {

		String nameOfParameter = "id";
		Condition condition = this.entityInformation.getIdExpression().isEqualTo(parameter(nameOfParameter));

		log.debug("Deleting entity with id {} ", id);

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, Optional.of(condition));
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.bind(id).to(nameOfParameter)
			.run();

		log.debug("Deleted {} nodes and {} relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted());
	}

	@Override
	@Transactional
	public void delete(T entity) {

		ID id = this.entityInformation.getId(entity);
		this.deleteById(id);
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		String nameOfParameter = "ids";
		Condition condition = entityInformation.getIdExpression().in(parameter(nameOfParameter));

		List<Object> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInformation::getId).collect(toList());

		log.debug("Deleting all entities with the following ids: {} ", ids);

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, Optional.of(condition));
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement))
			.bind(ids).to(nameOfParameter)
			.run();

		log.debug("Deleted {} nodes and {} relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted());
	}

	@Override
	@Transactional
	public void deleteAll() {

		log.debug("Deleting all nodes with primary label {}", entityMetaData.getPrimaryLabel());

		Statement statement = statementBuilder.prepareDeleteOf(entityMetaData, Optional.empty());
		ResultSummary summary = this.neo4jClient.query(renderer.render(statement)).run();

		log.debug("Deleted {} nodes and {} relationships.", summary.counters().nodesDeleted(),
			summary.counters().relationshipsDeleted());
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
