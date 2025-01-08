/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.SortItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.FluentFindOperation;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Querydsl specific fragment for extending {@link org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository}
 * with an implementation of {@link QuerydslPredicateExecutor}. Provides the necessary infrastructure for translating
 * Query-DSL predicates into conditions that are passed along to the Cypher-DSL and eventually to the template infrastructure.
 * This fragment will be loaded by the repository infrastructure when a repository is declared extending the above interface.
 *
 * @author Michael J. Simons
 * @param <T> The returned domain type.
 * @soundtrack Various - Chef Aid: The South Park Album
 * @since 6.1
 */
@API(status = API.Status.INTERNAL, since = "6.1")
public final class QuerydslNeo4jPredicateExecutor<T> implements QuerydslPredicateExecutor<T> {

	/**
	 * Non-fluent operations are translated directly into Cypherdsl conditions and executed elsewhere.
	 */
	private final CypherdslConditionExecutor<T> delegate;

	/**
	 * Needed to support the fluent operations.
	 */
	private final Neo4jOperations neo4jOperations;

	/**
	 * Needed to support the fluent operations.
	 */
	private final Neo4jPersistentEntity<T> metaData;

	/**
	 * Mapping context
	 */
	private final Neo4jMappingContext mappingContext;

	public QuerydslNeo4jPredicateExecutor(Neo4jMappingContext mappingContext, Neo4jEntityInformation<T, Object> entityInformation,
										  Neo4jOperations neo4jOperations) {

		this.mappingContext = mappingContext;
		this.delegate = new CypherdslConditionExecutorImpl<>(entityInformation, neo4jOperations);
		this.neo4jOperations = neo4jOperations;
		this.metaData = entityInformation.getEntityMetaData();
	}

	@Override
	public Optional<T> findOne(Predicate predicate) {

		return this.delegate.findOne(Cypher.adapt(predicate).asCondition());
	}

	@Override
	public Iterable<T> findAll(Predicate predicate) {

		return this.delegate.findAll(Cypher.adapt(predicate).asCondition());
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, Sort sort) {

		return this.delegate.findAll(Cypher.adapt(predicate).asCondition(), sort);
	}

	@Override
	public Iterable<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {

		return this.delegate.findAll(Cypher.adapt(predicate).asCondition(), toSortItems(orders));
	}

	@Override
	public Iterable<T> findAll(OrderSpecifier<?>... orders) {

		return this.delegate.findAll(toSortItems(orders));
	}

	@Override
	public Page<T> findAll(Predicate predicate, Pageable pageable) {

		return this.delegate.findAll(Cypher.adapt(predicate).asCondition(), pageable);
	}

	@Override
	public long count(Predicate predicate) {

		return this.delegate.count(Cypher.adapt(predicate).asCondition());
	}

	static SortItem[] toSortItems(OrderSpecifier<?>... orderSpecifiers) {

		return Arrays.stream(orderSpecifiers)
				.map(os -> Cypher.sort(Cypher.adapt(os.getTarget()).asExpression(),
						os.isAscending() ? SortItem.Direction.ASC : SortItem.Direction.DESC)).toArray(SortItem[]::new);

	}

	@Override
	public boolean exists(Predicate predicate) {
		return findAll(predicate).iterator().hasNext();
	}

	@Override
	public <S extends T, R> R findBy(Predicate predicate, Function<FetchableFluentQuery<S>, R> queryFunction) {

		if (this.neo4jOperations instanceof FluentFindOperation ops) {
			@SuppressWarnings("unchecked") // defaultResultType will be a supertype of S and at this stage, the same.
			FetchableFluentQuery<S> fluentQuery =
					(FetchableFluentQuery<S>) new FetchableFluentQueryByPredicate<>(predicate, mappingContext, metaData, metaData.getType(),
							ops, this::count, this::exists);
			return queryFunction.apply(fluentQuery);
		}
		throw new UnsupportedOperationException(
				"Fluent find by predicate not supported with standard Neo4jOperations, must support fluent queries too");
	}
}
