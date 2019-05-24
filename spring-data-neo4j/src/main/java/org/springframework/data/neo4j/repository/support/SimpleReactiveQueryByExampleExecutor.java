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

import static lombok.AccessLevel.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;
import static org.springframework.data.neo4j.repository.query.CypherAdapterUtils.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient.ExecutableQuery;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;

/**
 * A fragment for repositories providing "Query by example" functionality in a reactive way.
 *
 * @param <T>
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor(access = PACKAGE)
class SimpleReactiveQueryByExampleExecutor<T> implements ReactiveQueryByExampleExecutor<T> {

	private static final Renderer renderer = CypherRenderer.create();

	private final ReactiveNeo4jClient neo4jClient;

	private final Neo4jMappingContext mappingContext;

	private final SchemaBasedStatementBuilder statementBuilder;

	@Override
	public <S extends T> Mono<S> findOne(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.f(statementBuilder::prepareMatchOf)
			.returning(asterisk())
			.build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.getParameters()).getSingleResult();
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.f(statementBuilder::prepareMatchOf)
			.returning(asterisk())
			.build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.getParameters()).getResults();
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example, Sort sort) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.f(statementBuilder::prepareMatchOf)
			.returning(asterisk())
			.orderBy(toSortItems(predicate.getNodeDescription(), sort)).build();

		return createExecutableQuery(example.getProbeType(), statement, predicate.getParameters()).getResults();
	}

	@Override
	public <S extends T> Mono<Long> count(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.f(statementBuilder::prepareMatchOf)
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
			= this.mappingContext.getMappingFunctionFor(resultType).orElse(null);

		PreparedQuery preparedQuery = PreparedQuery.queryFor(resultType)
			.withCypherQuery(renderer.render(statement))
			.withParameters(parameters)
			.usingMappingFunction(mappingFunctionToUse)
			.build();

		return neo4jClient.toExecutableQuery(preparedQuery);
	}
}
