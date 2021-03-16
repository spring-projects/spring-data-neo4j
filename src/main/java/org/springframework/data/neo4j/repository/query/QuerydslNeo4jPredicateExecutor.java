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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;
import org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.support.PageableExecutionUtils;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Querydsl specific fragment for extending {@link SimpleNeo4jRepository} with an implementation of {@link QuerydslPredicateExecutor}.
 * Provides the necessary infrastructure for translating Query-DSL predicates into conditions that are passed along
 * to the Cypher-DSL and eventually to the template infrastructure. This fragment will be loaded by the repository
 * infrastructure when
 *
 * @param <T> The returned domain type.
 * @author Michael J. Simons
 * @soundtrack Various - Chef Aid: The South Park Album
 * @since 6.1
 */
@API(status = API.Status.INTERNAL, since = "6.1")
public final class QuerydslNeo4jPredicateExecutor<T> implements QuerydslPredicateExecutor<T> {

	private final Neo4jEntityInformation<T, Object> entityInformation;

	private final Neo4jOperations neo4jOperations;

	private final Neo4jPersistentEntity<T> metaData;

	public QuerydslNeo4jPredicateExecutor(Neo4jEntityInformation<T, Object> entityInformation,
			Neo4jOperations neo4jOperations) {

		this.entityInformation = entityInformation;
		this.neo4jOperations = neo4jOperations;
		this.metaData = this.entityInformation.getEntityMetaData();
	}

	@Override
	public Optional<T> findOne(Predicate predicate) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Cypher.adapt(predicate).asCondition(), null, null)
		).getSingleResult();
	}

	@Override
	public Iterable<T> findAll(Predicate predicate) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Cypher.adapt(predicate).asCondition(), null, null)
		).getResults();
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, Sort sort) {

		return this.neo4jOperations.toExecutableQuery(
				metaData.getType(),
				QueryFragmentsAndParameters.forCondition(
						this.metaData, Cypher.adapt(predicate).asCondition(), null, CypherAdapterUtils.toSortItems(this.metaData, sort)
				)
		).getResults();
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orderSpecifiers) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(
						this.metaData, Cypher.adapt(predicate).asCondition(), null, toSortItems(orderSpecifiers)
				)
		).getResults();
	}

	@Override
	public Iterable<T> findAll(OrderSpecifier<?>... orderSpecifiers) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Conditions.noCondition(), null, toSortItems(orderSpecifiers))
		).getResults();
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		List<T> page = this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Conditions.noCondition(), pageable, null)
		).getResults();
		LongSupplier totalCountSupplier = () -> this.count(predicate);
		return PageableExecutionUtils.getPage(page, pageable, totalCountSupplier);
	}

	@Override
	public long count(Predicate predicate) {

		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(this.metaData, Cypher.adapt(predicate).asCondition())
				.returning(Functions.count(asterisk())).build();
		return this.neo4jOperations.count(statement, statement.getParameters());
	}

	private SortItem[] toSortItems(OrderSpecifier<?>... orderSpecifiers) {

		return Arrays.stream(orderSpecifiers)
				.map(os -> Cypher.sort(Cypher.adapt(os.getTarget()).asExpression(),
						os.isAscending() ? SortItem.Direction.ASC : SortItem.Direction.DESC)).toArray(SortItem[]::new);

	}

	@Override
	public boolean exists(Predicate predicate) {
		return findAll(predicate).iterator().hasNext();
	}
}
