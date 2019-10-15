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

import org.neo4j.springframework.data.core.ReactiveNeo4jOperations;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.schema.CypherGenerator;
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

	private final ReactiveNeo4jOperations neo4jOperations;

	private final Neo4jMappingContext mappingContext;

	private final CypherGenerator cypherGenerator;

	SimpleReactiveQueryByExampleExecutor(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext) {

		this.neo4jOperations = neo4jOperations;
		this.mappingContext = mappingContext;
		this.cypherGenerator = CypherGenerator.INSTANCE;
	}

	@Override
	public <S extends T> Mono<S> findOne(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf)
			.returning(asterisk())
			.build();

		return this.neo4jOperations.findOne(statement, predicate.getParameters(), example.getProbeType());
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf)
			.returning(asterisk())
			.build();

		return this.neo4jOperations.findAll(statement, predicate.getParameters(), example.getProbeType());
	}

	@Override
	public <S extends T> Flux<S> findAll(Example<S> example, Sort sort) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf)
			.returning(asterisk())
			.orderBy(toSortItems(predicate.getNeo4jPersistentEntity(), sort)).build();

		return this.neo4jOperations.findAll(statement, predicate.getParameters(), example.getProbeType());
	}

	@Override
	public <S extends T> Mono<Long> count(Example<S> example) {

		Predicate predicate = Predicate.create(mappingContext, example);
		Statement statement = predicate.useWithReadingFragment(cypherGenerator::prepareMatchOf)
			.returning(Functions.count(asterisk()))
			.build();

		return this.neo4jOperations.count(statement, predicate.getParameters());
	}

	@Override
	public <S extends T> Mono<Boolean> exists(Example<S> example) {
		return findAll(example).hasElements();
	}
}
