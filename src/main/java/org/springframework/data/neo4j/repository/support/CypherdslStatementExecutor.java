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

import java.util.Collection;
import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingAndReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * An interface that can be added to any imperative repository so that the repository exposes several methods taking in
 * a {@link Statement} from the Cypher-DSL, that allows for full customization of the queries executed in a programmatic
 * way in contrast to provide custom queries declaratively via {@link org.springframework.data.neo4j.repository.query.Query @Query}
 * annotations.

 * @author Michael J. Simons
 * @param <T> The domain type of the repository
 * @soundtrack Queen - Queen On Air
 * @since 6.1
 */
@API(status = API.Status.STABLE, since = "6.1")
public interface CypherdslStatementExecutor<T> {

	/**
	 * Find one element of the domain as defined by the {@code statement}. The statement must return either no or exactly
	 * one mappable record.
	 *
	 * @param statement A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @return An empty optional or an optional containing the single element
	 */
	Optional<T> findOne(Statement statement);

	/**
	 * Creates a custom projection of the repository type by a Cypher-DSL based statement. The statement must return either
	 * no or exactly one mappable record.
	 *
	 * @param statement       A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @param projectionClass The class of the projection type
	 * @param <PT>            The type of the projection
	 * @return An empty optional or an optional containing the single, projected element
	 */
	<PT> Optional<PT> findOne(Statement statement, Class<PT> projectionClass);

	/**
	 * Find all elements of the domain as defined by the {@code statement}.
	 *
	 * @param statement A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @return An iterable full of domain objects
	 */
	Collection<T> findAll(Statement statement);

	/**
	 * Creates a custom projection of the repository type by a Cypher-DSL based statement.
	 *
	 * @param statement       A full Cypher statement, matching and returning all required nodes, relationships and properties
	 * @param projectionClass The class of the projection type
	 * @param <PT>            The type of the projection
	 * @return An iterable full of projections
	 */
	<PT> Collection<PT> findAll(Statement statement, Class<PT> projectionClass);

	/**
	 * The pages here are build with a fragment of a {@link Statement}: An
	 * {@link OngoingReadingAndReturn ongoing reading with an attached return}. The next step is ordering the results,
	 * and that order will be derived from the {@code pageable}. The same applies than for the values of skip and limit.
	 *
	 * @param statement       The almost complete statement that actually matches and returns the nodes and relationships to be projected
	 * @param countQuery      The statement that is executed to count the total number of matches for computing the correct number of pages
	 * @param pageable        The definition of the page
	 * @return A page full of domain objects
	 */
	Page<T> findAll(OngoingReadingAndReturn statement, Statement countQuery, Pageable pageable);

	/**
	 * The pages here are build with a fragment of a {@link Statement}: An
	 * {@link OngoingReadingAndReturn ongoing reading with an attached return}. The next step is ordering the results,
	 * and that order will be derived from the {@code pageable}. The same applies than for the values of skip and limit.
	 *
	 * @param statement       The almost complete statement that actually matches and returns the nodes and relationships to be projected
	 * @param countQuery      The statement that is executed to count the total number of matches for computing the correct number of pages
	 * @param pageable        The definition of the page
	 * @param projectionClass The class of the projection type
	 * @param <PT>            The type of the projection
	 * @return A page full of projections
	 */
	<PT> Page<PT> findAll(OngoingReadingAndReturn statement, Statement countQuery, Pageable pageable, Class<PT> projectionClass);
}
