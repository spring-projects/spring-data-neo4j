/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.repository.support;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apiguardian.api.API;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Repository base implementation for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Ján Šúr
 * @author Jens Schauder
 * @param <T> the type of the domain class managed by this repository
 * @param <ID> the type of the unique identifier of the domain class
 * @since 6.0
 */
@Repository
@Transactional(readOnly = true)
@API(status = API.Status.STABLE, since = "6.0")
public class SimpleNeo4jRepository<T, ID> implements PagingAndSortingRepository<T, ID> {

	private final Neo4jOperations neo4jOperations;

	private final Neo4jEntityInformation<T, ID> entityInformation;

	private final Neo4jPersistentEntity<T> entityMetaData;

	protected SimpleNeo4jRepository(Neo4jOperations neo4jOperations, Neo4jEntityInformation<T, ID> entityInformation) {

		this.neo4jOperations = neo4jOperations;
		this.entityInformation = entityInformation;
		this.entityMetaData = this.entityInformation.getEntityMetaData();
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

		return this.neo4jOperations.toExecutableQuery(entityInformation.getJavaType(),
				QueryFragmentsAndParameters.forPageableAndSort(entityMetaData, null, sort))
				.getResults();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		List<T> allResult = this.neo4jOperations.toExecutableQuery(entityInformation.getJavaType(),
				QueryFragmentsAndParameters.forPageableAndSort(entityMetaData, pageable, null))
				.getResults();

		LongSupplier totalCountSupplier = this::count;
		return PageableExecutionUtils.getPage(allResult, pageable, totalCountSupplier);
	}

	@Override
	public long count() {

		return neo4jOperations.count(this.entityInformation.getJavaType());
	}

	@Override
	public boolean existsById(ID id) {

		return this.neo4jOperations.existsById(id, this.entityInformation.getJavaType());
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
		Assert.notNull(id, "Cannot delete individual nodes without an id.");
		if (entityMetaData.hasVersionProperty()) {
			Neo4jPersistentProperty versionProperty = entityMetaData.getRequiredVersionProperty();
			Object versionValue = entityMetaData.getPropertyAccessor(entity).getProperty(versionProperty);
			this.neo4jOperations.deleteByIdWithVersion(id, this.entityInformation.getJavaType(), versionProperty, versionValue);
		} else {
			this.deleteById(id);
		}
	}

	@Override
	@Transactional
	public void deleteAllById(Iterable<? extends ID> ids) {

		this.neo4jOperations.deleteAllById(ids, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void deleteAll(Iterable<? extends T> entities) {

		List<Object> ids = StreamSupport.stream(entities.spliterator(), false).map(this.entityInformation::getId)
				.collect(Collectors.toList());

		this.neo4jOperations.deleteAllById(ids, this.entityInformation.getJavaType());
	}

	@Override
	@Transactional
	public void deleteAll() {

		this.neo4jOperations.deleteAll(this.entityInformation.getJavaType());
	}
}
