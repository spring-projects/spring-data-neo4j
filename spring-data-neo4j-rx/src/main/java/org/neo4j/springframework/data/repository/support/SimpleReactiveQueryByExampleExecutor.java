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

import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.repository.query.CypherAdapterUtils.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.PreparedQuery;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient.ExecutableQuery;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;

/**
 * A fragment for repositories providing "Query by example" functionality in a reactive way.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @param <T> type of the domain class
 * @since 1.0
 */
class SimpleReactiveQueryByExampleExecutor<T> implements ReactiveQueryByExampleExecutor<T> {

	private static final Renderer renderer = Renderer.getDefaultRenderer();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext mappingContext;

	private final SchemaBasedStatementBuilder statementBuilder;

	SimpleReactiveQueryByExampleExecutor(ReactiveNeo4jClient neo4jClient, Neo4jMappingContext mappingContext,
		SchemaBasedStatementBuilder statementBuilder) {
		this.neo4jClient = neo4jClient;
		this.mappingContext = mappingContext;
		this.statementBuilder = statementBuilder;
	}

	@Override
	public <S extends T> Mono<S> findOne(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(statementBuilder::prepareMatchOf)
			.returning(asterisk())
			.build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.getParameters()).getSingleResult();
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(statementBuilder::prepareMatchOf)
			.returning(asterisk())
			.build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.getParameters()).getResults();
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example, Sort sort) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(statementBuilder::prepareMatchOf)
			.returning(asterisk())
			.orderBy(toSortItems(predicate.getNodeDescription(), sort)).build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.getParameters()).getResults();
	}

	@Override
	public <S extends T> Mono<Long> count(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(statementBuilder::prepareMatchOf)
			.returning(Functions.count(asterisk()))
			.build();

		return createExecutableQuery(Long.class, statement, predicate.getParameters()).getSingleResult();
	}

	@Override
	public <S extends T> Mono<Boolean> exists(Example<S> example) {
		return findAll(example).hasElements();
	}

	private <RS> ExecutableQuery<RS> createExecutableQuery(Class<RS> resultType, Statement statement,
		Map<String, Object> parameters) {

		BiFunction<TypeSystem, Record, ?> mappingFunctionToUse
			= this.mappingContext.getMappingFunctionFor(resultType);

		PreparedQuery preparedQuery = PreparedQuery.queryFor(resultType)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(mappingFunctionToUse)
			.build();

		return neo4jClient.toExecutableQuery(preparedQuery);
	}
}
