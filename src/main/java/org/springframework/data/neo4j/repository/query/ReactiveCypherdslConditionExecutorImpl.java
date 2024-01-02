/*
 * Copyright 2011-2024 the original author or authors.
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

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.repository.support.ReactiveCypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.Neo4jEntityInformation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Niklas Krieger
 * @author Michael J. Simons
 * @param <T> The returned domain type.
 * @since 6.3.3
 */
@API(status = API.Status.INTERNAL, since = "6.3.3")
public final class ReactiveCypherdslConditionExecutorImpl<T> implements ReactiveCypherdslConditionExecutor<T> {

	private final Neo4jEntityInformation<T, Object> entityInformation;

	private final ReactiveNeo4jOperations neo4jOperations;

	private final Neo4jPersistentEntity<T> metaData;

	public ReactiveCypherdslConditionExecutorImpl(Neo4jEntityInformation<T, Object> entityInformation,
			ReactiveNeo4jOperations neo4jOperations) {

		this.entityInformation = entityInformation;
		this.neo4jOperations = neo4jOperations;
		this.metaData = this.entityInformation.getEntityMetaData();
	}

	@Override
	public Mono<T> findOne(Condition condition) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, condition, null, null)
		).flatMap(ReactiveNeo4jOperations.ExecutableQuery::getSingleResult);
	}

	@Override
	public Flux<T> findAll(Condition condition) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, condition, null, null)
		).flatMapMany(ReactiveNeo4jOperations.ExecutableQuery::getResults);
	}

	@Override
	public Flux<T> findAll(Condition condition, Sort sort) {

		return this.neo4jOperations.toExecutableQuery(
				metaData.getType(),
				QueryFragmentsAndParameters.forCondition(
						this.metaData, condition, null, CypherAdapterUtils.toSortItems(this.metaData, sort)
				)
		).flatMapMany(ReactiveNeo4jOperations.ExecutableQuery::getResults);
	}

	@Override
	public Flux<T> findAll(Condition condition, SortItem... sortItems) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(
						this.metaData, condition, null, Arrays.asList(sortItems)
				)
		).flatMapMany(ReactiveNeo4jOperations.ExecutableQuery::getResults);
	}

	@Override
	public Flux<T> findAll(SortItem... sortItems) {

		return this.neo4jOperations.toExecutableQuery(
				this.metaData.getType(),
				QueryFragmentsAndParameters.forCondition(this.metaData, Conditions.noCondition(), null,
						Arrays.asList(sortItems))
		).flatMapMany(ReactiveNeo4jOperations.ExecutableQuery::getResults);
	}

	@Override
	public Mono<Long> count(Condition condition) {

		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(this.metaData, condition)
				.returning(Functions.count(asterisk())).build();
		return this.neo4jOperations.count(statement, statement.getParameters());
	}

	@Override
	public Mono<Boolean> exists(Condition condition) {
		return count(condition).map(count -> count > 0);
	}
}
