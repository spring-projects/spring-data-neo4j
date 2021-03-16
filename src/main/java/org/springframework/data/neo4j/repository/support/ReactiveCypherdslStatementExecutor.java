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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Statement;

/**
 * An interface that can be added to any reactive repository so that the repository exposes several methods taking in
 * a {@link Statement} from the Cypher-DSL, that allows for full customization of the queries executed in a programmatic
 * way in contrast to provide custom queries declaratively via {@link org.springframework.data.neo4j.repository.query.Query @Query}
 * annotations.
 *
 * @param <T> The domain type of the repository
 * @author Michael J. Simons
 * @soundtrack Queen - Queen On Air
 * @since 6.1
 */
@API(status = API.Status.STABLE, since = "6.1")
public interface ReactiveCypherdslStatementExecutor<T> {

	/**
	 * Find one element of the domain as defined by the {@code statement}. The statement must return either no or exactly
	 * one mappable record.
	 *
	 * @param statement A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @return An empty Mono or a Mono containing the single element
	 */
	Mono<T> findOne(Statement statement);

	/**
	 * Creates a custom projection of the repository type by a Cypher-DSL based statement. The statement must return either
	 * no or exactly one mappable record.
	 *
	 * @param statement       A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @param projectionClass The class of the projection type
	 * @param <PT>            The type of the projection
	 * @return An empty Mono or a Mono containing the single, projected element
	 */
	<PT> Mono<PT> findOne(Statement statement, Class<PT> projectionClass);

	/**
	 * Find all elements of the domain as defined by the {@code statement}.
	 *
	 * @param statement A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @return A publisher full of domain objects
	 */
	Flux<T> findAll(Statement statement);

	/**
	 * Creates a custom projection of the repository type by a Cypher-DSL based statement.
	 *
	 * @param statement       A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @param projectionClass The class of the projection type
	 * @param <PT>            The type of the projection
	 * @return A publisher full of projections
	 */
	<PT> Flux<PT> findAll(Statement statement, Class<PT> projectionClass);
}
