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

import static java.util.stream.Collectors.*;
import static org.neo4j.springframework.data.repository.query.CypherAdapterUtils.*;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.StreamSupport;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.Neo4jOperations;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.StatementBuilder;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.schema.CypherGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository base implementation for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Ján Šúr
 * @since 1.0
 * @param <T> the type of the domain class managed by this repository
 * @param <ID> the type of the unique identifier of the domain class
 */
@Repository
@Transactional(readOnly = true)
@API(status = API.Status.STABLE, since = "1.0")
public class SimpleNeo4jRepository<T, ID> implements PagingAndSortingRepository<T, ID> {

	private final Neo4jOperations neo4jOperations;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final Neo4jPersistentEntity<T> entityMetaData;

	private final CypherGenerator cypherGenerator;

	protected SimpleNeo4jRepository(Neo4jOperations neo4jOperations, Neo4jEntityInformation<T, ID> entityInformation) {

		this.neo4jOperations = neo4jOperations;
		this.entityInformation = entityInformation;
		this.entityMetaData = this.entityInformation.getEntityMetaData();
		this.cypherGenerator = CypherGenerator.INSTANCE;
	}

	@Override
	public Optional<T> findById(ID id) {

		return neo4jOperations.findById(id, this.entityInformation.getJavaType());
	}

	@Override
	public List<T> findAllById(Iterable<ID> ids) {

		return neo4jOperations.findAllById(ids, this.entityInformation.getJavaType());
	}

	@Override
	public List<T> findAll() {

		return this.neo4jOperations.findAll(this.entityInformation.getJavaType());
	}

	@Override
	public List<T> findAll(Sort sort) {

		Statement statement = cypherGenerator.prepareMatchOf(entityMetaData)
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData))
			.orderBy(toSortItems(entityMetaData, sort))
			.build();

		return this.neo4jOperations.findAll(statement, entityInformation.getJavaType());
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		OngoingReadingAndReturn returning = cypherGenerator.prepareMatchOf(entityMetaData)
			.returning(cypherGenerator.createReturnStatementForMatch(entityMetaData));

		StatementBuilder.BuildableStatement returningWithPaging =
			addPagingParameter(entityMetaData, pageable, returning);

		Statement statement = returningWithPaging.build();

		List<T> allResult = this.neo4jOperations.findAll(statement, entityInformation.getJavaType());
		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	public long count() {

		return neo4jOperations.count(this.entityInformation.getJavaType());
	}

	@Override
	public boolean existsById(ID id) {
		return findById(id).isPresent();
	}

	@Override
	@Transactional
	public <S extends T> S save(S entity) {

		return this.neo4jOperations.save(entity);
	}

	@Override
	@Transactional
	public <S extends T> List<S> saveAll(Iterable<S> entities) {

		return this.neo4jOperations.saveAll(entities);
	}

	@Override
	@Transactional
	public void deleteById(ID id) {

		this.neo4jOperations.deleteById(id, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void delete(T entity) {

		ID id = this.entityInformation.getId(entity);
		this.deleteById(id);
	}

	@Override
	@Transactional
	public void deleteAll() {

		this.neo4jOperations.deleteAll(this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		List<Object> ids = StreamSupport.stream(entities.spliterator(), false)
			.map(this.entityInformation::getId).collect(toList());

		this.neo4jOperations.deleteAllById(ids, this.entityInformation.getJavaType());
	}
}
