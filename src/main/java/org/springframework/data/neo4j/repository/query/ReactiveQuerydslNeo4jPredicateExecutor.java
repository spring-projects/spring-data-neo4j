/*
 * Copyright 2011-2022 the original author or authors.
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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.ReactiveFluentFindOperation;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery.ReactiveFluentQuery;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;

/**
 * Querydsl specific fragment for extending {@link org.springframework.data.neo4j.repository.support.SimpleReactiveNeo4jRepository}
 * with an implementation of {@link ReactiveQuerydslPredicateExecutor}. Provides the necessary infrastructure for translating
 * Query-DSL predicates into conditions that are passed along to the Cypher-DSL and eventually to the template infrastructure.
 * This fragment will be loaded by the repository infrastructure when a repository is declared extending the above interface.
 *
 * @author Michael J. Simons
 * @param <T> The returned domain type.
 * @since 6.2
 */
@API(status = API.Status.INTERNAL, since = "6.2")
public final class ReactiveQuerydslNeo4jPredicateExecutor<T> implements ReactiveQuerydslPredicateExecutor<T> {

	private final Neo4jEntityInformation<T, Object> entityInformation;

	private final ReactiveNeo4jOperations neo4jOperations;

	private final Neo4jPersistentEntity<T> metaData;

	public ReactiveQuerydslNeo4jPredicateExecutor(Neo4jEntityInformation<T, Object> entityInformation,
			ReactiveNeo4jOperations neo4jOperations) {

		this.entityInformation = entityInformation;
		this.neo4jOperations = neo4jOperations;
		this.metaData = this.entityInformation.getEntityMetaData();
	}

	@Override
	public Mono<T> findOne(Predicate predicate) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Cypher.adapt(predicate).asCondition(), null,
						null)
		).flatMap(ReactiveNeo4jOperations.ExecutableQuery::getSingleResult);
	}

	@Override
	public Flux<T> findAll(Predicate predicate) {

		return doFindAll(Cypher.adapt(predicate).asCondition(), null);
	}

	@Override
	public Flux<T> findAll(Predicate predicate, Sort sort) {

		return doFindAll(Cypher.adapt(predicate).asCondition(), CypherAdapterUtils.toSortItems(this.metaData, sort));
	}

	@Override
	public Flux<T> findAll(Predicate predicate, OrderSpecifier<?>... orders) {
		return doFindAll(Cypher.adapt(predicate).asCondition(), Arrays.asList(QuerydslNeo4jPredicateExecutor.toSortItems(orders)));

	}

	@Override
	public Flux<T> findAll(OrderSpecifier<?>... orders) {

		return doFindAll(Conditions.noCondition(), Arrays.asList(QuerydslNeo4jPredicateExecutor.toSortItems(orders)));
	}

	private Flux<T> doFindAll(Condition condition, Collection<SortItem> sortItems) {
		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, condition, null,
						sortItems)
		).flatMapMany(ReactiveNeo4jOperations.ExecutableQuery::getResults);
	}

	@Override
	public Mono<Long> count(Predicate predicate) {

		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(this.metaData,
						Cypher.adapt(predicate).asCondition())
				.returning(Functions.count(asterisk())).build();
		return this.neo4jOperations.count(statement, statement.getParameters());
	}

	@Override
	public Mono<Boolean> exists(Predicate predicate) {
		return findAll(predicate).hasElements();
	}

	@Override
	public <S extends T, R, P extends Publisher<R>> P findBy(Predicate predicate, Function<ReactiveFluentQuery<S>, P> queryFunction) {

		if (this.neo4jOperations instanceof ReactiveFluentFindOperation ops) {
			@SuppressWarnings("unchecked") // defaultResultType will be a supertype of S and at this stage, the same.
			ReactiveFluentQuery<S> fluentQuery = (ReactiveFluentQuery<S>) new ReactiveFluentQueryByPredicate<>(predicate, metaData, metaData.getType(),
							ops, this::count, this::exists);
			return queryFunction.apply(fluentQuery);
		}
		throw new UnsupportedOperationException(
				"Fluent find by example not supported with standard Neo4jOperations, must support fluent queries too");
	}
}
