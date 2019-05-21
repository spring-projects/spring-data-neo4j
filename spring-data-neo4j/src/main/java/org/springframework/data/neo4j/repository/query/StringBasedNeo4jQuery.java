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
package org.springframework.data.neo4j.repository.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.support.PreparedQuery;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.SpelEvaluator;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.data.repository.query.SpelQueryContext.SpelExtractor;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link RepositoryQuery} for query methods annotated with {@link Query @Query}.
 *
 *
 * The flow to handle queries with SpEL parameters is as follows
 * <ol>
 * <li>Parse template as something that has SpEL-expressions in it</li>
 * <li>Replace the SpEL-expressions with Neo4j Statement template parameters</li>
 * <li>The parameters passed here _and_ the values that might have been computed during SpEL-parsing</li>
 * </ol>
 * The main ingredient is a SpelEvaluator, that parses a template and replaces SpEL expressions
 * with real Neo4j parameters.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
final class StringBasedNeo4jQuery extends AbstractNeo4jQuery {

	/**
	 * Used for extracting SpEL expressions inside Cypher query templates.
	 */
	static final SpelQueryContext SPEL_QUERY_CONTEXT = SpelQueryContext
		.of(StringBasedNeo4jQuery::parameterNameSource, StringBasedNeo4jQuery::replacementSource);

	/**
	 * Is this a count projection?
	 */
	private final boolean countQuery;

	/**
	 * Is this an exists projection?
	 */
	private final boolean existsQuery;

	/**
	 * Is this a modifying delete query?
	 */
	private final boolean deleteQuery;

	/**
	 * Used to evaluate the expression found while parsing the cypher template of this query against the actual parameters
	 * with the help of the formal parameters during the building of the {@link PreparedQuery}.
	 */
	private final SpelEvaluator spelEvaluator;

	/**
	 * The Cypher string used for this query. The cypher query will not be changed after parsed via {@link #SPEL_QUERY_CONTEXT}.
	 * All SpEL expressions will be substituted via "native" parameter placeholders. This will be done via the {@link #spelEvaluator}.
	 */
	private final String cypherQuery;

	/**
	 * Create a {@link StringBasedNeo4jQuery} for a query method that is annotated with {@link Query @Query}. The annotation
	 * is expected to have a value.
	 *
	 * @param neo4jClient
	 * @param mappingContext
	 * @param evaluationContextProvider
	 * @param queryMethod
	 * @return A new instance of a String based Neo4j query.
	 */
	static StringBasedNeo4jQuery create(Neo4jClient neo4jClient, Neo4jMappingContext mappingContext,
		QueryMethodEvaluationContextProvider evaluationContextProvider,
		Neo4jQueryMethod queryMethod) {

		Query queryAnnotation = queryMethod.getQueryAnnotation()
			.orElseThrow(() -> new MappingException("Expected @Query annotation on the query method!"));

		String cypherTemplate = Optional.ofNullable(queryAnnotation.value())
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new MappingException("Expected @Query annotation to have a value, but it did not."));

		return new StringBasedNeo4jQuery(neo4jClient, mappingContext, evaluationContextProvider, queryMethod,
			cypherTemplate, queryAnnotation.count(), queryAnnotation.exists(), queryAnnotation.delete());
	}

	/**
	 * Create a {@link StringBasedNeo4jQuery} based on an explicit Cypher template.
	 *
	 * @param neo4jClient
	 * @param mappingContext
	 * @param evaluationContextProvider
	 * @param queryMethod
	 * @param cypherTemplate            The template to use.
	 * @return A new instance of a String based Neo4j query.
	 */
	static StringBasedNeo4jQuery create(Neo4jClient neo4jClient, Neo4jMappingContext mappingContext,
		QueryMethodEvaluationContextProvider evaluationContextProvider,
		Neo4jQueryMethod queryMethod, String cypherTemplate) {

		Assert.hasText(cypherTemplate, "Cannot create String based Neo4j query without a cypher template.");

		return new StringBasedNeo4jQuery(neo4jClient, mappingContext, evaluationContextProvider, queryMethod,
			cypherTemplate, false, false, false);
	}

	private StringBasedNeo4jQuery(Neo4jClient neo4jClient,
		Neo4jMappingContext mappingContext, QueryMethodEvaluationContextProvider evaluationContextProvider,
		Neo4jQueryMethod queryMethod, String cypherTemplate, boolean countQuery,
		boolean existsQuery, boolean deleteQuery) {

		super(neo4jClient, mappingContext, queryMethod);

		this.countQuery = countQuery;
		this.existsQuery = existsQuery;
		this.deleteQuery = deleteQuery;

		SpelExtractor spelExtractor = SPEL_QUERY_CONTEXT.parse(cypherTemplate);
		this.spelEvaluator = new SpelEvaluator(evaluationContextProvider, queryMethod.getParameters(), spelExtractor);
		this.cypherQuery = spelExtractor.getQueryString();
	}

	static String getQueryTemplate(Query queryAnnotation) {

		return Optional.ofNullable(queryAnnotation.value())
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new MappingException("Expected @Query annotation to have a value, but it did not."));
	}

	@Override
	protected PreparedQuery<?> prepareQuery(Object[] parameters) {

		return PreparedQuery.queryFor(super.domainType)
			.withCypherQuery(cypherQuery)
			.withParameters(bindParameters(parameters))
			.usingMappingFunction(mappingContext.getMappingFunctionFor(super.domainType).orElse(null)) // Null is fine
			.build();
	}

	@Override
	public boolean isCountQuery() {
		return countQuery;
	}

	@Override
	public boolean isExistsQuery() {
		return existsQuery;
	}

	@Override
	public boolean isDeleteQuery() {
		return deleteQuery;
	}

	@Override
	protected boolean isLimiting() {
		return false;
	}

	Map<String, Object> bindParameters(Object[] actualParameters) {

		final Parameters<?, ?> formalParameters = queryMethod.getParameters();

		Map<String, Object> resolvedParameters = new HashMap<>(spelEvaluator.evaluate(actualParameters));
		formalParameters.stream()
			.filter(Parameter::isBindable)
			.forEach(parameter -> {

				int parameterIndex = parameter.getIndex();
				Object parameterValue = super.convertParameter(actualParameters[parameterIndex]);

				// Add the parameter under its name when possible
				parameter.getName()
					.ifPresent(parameterName -> resolvedParameters.put(parameterName, parameterValue));
				// Always add under its index.
				resolvedParameters.put(Integer.toString(parameterIndex), parameterValue);
			});

		return resolvedParameters;
	}

	/**
	 * @param index
	 * @param originalSpelExpression Not used for configuring parameter names atm.
	 * @return
	 */
	private static String parameterNameSource(int index, @SuppressWarnings("unused") String originalSpelExpression) {
		return "__SpEL__" + index;
	}

	/**
	 * @param originalPrefix The prefix passed to the replacement source is either ':' or '?', so that isn't usable for
	 *                       Cypher templates and therefore ignored.
	 * @param parameterName
	 * @return
	 */
	private static String replacementSource(@SuppressWarnings("unused") String originalPrefix, String parameterName) {
		return "$" + parameterName;
	}
}
