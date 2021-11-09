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
package org.springframework.data.neo4j.repository.query;

import static org.neo4j.cypherdsl.core.Cypher.asterisk;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;
import org.springframework.data.support.PageableExecutionUtils;

/**
 * @author Michael J. Simons
 * @param <T> The returned domain type.
 * @since 6.1
 */
@API(status = API.Status.INTERNAL, since = "6.1")
public final class CypherdslConditionExecutorImpl<T> implements CypherdslConditionExecutor<T> {

	private final Neo4jEntityInformation<T, Object> entityInformation;

	private final Neo4jOperations neo4jOperations;

	private final Neo4jPersistentEntity<T> metaData;

	public CypherdslConditionExecutorImpl(Neo4jEntityInformation<T, Object> entityInformation,
			Neo4jOperations neo4jOperations) {

		this.entityInformation = entityInformation;
		this.neo4jOperations = neo4jOperations;
		this.metaData = this.entityInformation.getEntityMetaData();
	}

	@Override
	public Optional<T> findOne(Condition condition) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, condition, null, null)
		).getSingleResult();
	}

	@Override
	public Collection<T> findAll(Condition condition) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, condition, null, null)
		).getResults();
	}

	@Override
	public Collection<T> findAll(Condition condition, Sort sort) {

		return this.neo4jOperations.toExecutableQuery(
				metaData.getType(),
				QueryFragmentsAndParameters.forCondition(
						this.metaData, condition, null, CypherAdapterUtils.toSortItems(this.metaData, sort)
				)
		).getResults();
	}

	@Override
	public Collection<T> findAll(Condition condition, SortItem... sortItems) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(
						this.metaData, condition, null, Arrays.asList(sortItems)
				)
		).getResults();
	}

	@Override
	public Collection<T> findAll(SortItem... sortItems) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Conditions.noCondition(), null, Arrays.asList(sortItems))
		).getResults();
	}

	@Override
	public Page<T> findAll(Condition condition, Pageable pageable) {

		List<T> page = this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, condition, pageable, null)
		).getResults();
		LongSupplier totalCountSupplier = () -> this.count(condition);
		return PageableExecutionUtils.getPage(page, pageable, totalCountSupplier);
	}

	@Override
	public long count(Condition condition) {

		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(this.metaData, condition)
				.returning(Functions.count(asterisk())).build();
		return this.neo4jOperations.count(statement, statement.getParameters());
	}

	@Override
	public boolean exists(Condition condition) {
		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(this.metaData, condition)
				.returning(Functions.count(asterisk())).build();
		return this.neo4jOperations.count(statement, statement.getParameters()) > 0;
	}
}
