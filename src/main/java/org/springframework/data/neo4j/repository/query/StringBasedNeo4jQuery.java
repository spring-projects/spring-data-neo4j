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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.query.Neo4jSpelSupport.LiteralReplacement;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.SpelEvaluator;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link RepositoryQuery} for query methods annotated with {@link Query @Query}. The flow to handle
 * queries with SpEL parameters is as follows
 * <ol>
 * <li>Parse template as something that has SpEL-expressions in it</li>
 * <li>Replace the SpEL-expressions with Neo4j Statement template parameters</li>
 * <li>The parameters passed here _and_ the values that might have been computed during SpEL-parsing</li>
 * </ol>
 * The main ingredient is a SpelEvaluator, that parses a template and replaces SpEL expressions with real Neo4j
 * parameters.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class StringBasedNeo4jQuery extends AbstractNeo4jQuery {

	/**
	 * Used for extracting SpEL expressions inside Cypher query templates.
	 */
	static final SpelQueryContext SPEL_QUERY_CONTEXT = SpelQueryContext.of(StringBasedNeo4jQuery::parameterNameSource,
			StringBasedNeo4jQuery::replacementSource);

	private final static String COMMENT_OR_WHITESPACE_GROUP = "(?:\\s|/\\\\*.*?\\\\*/|//.*?$)";
	static final Pattern SKIP_AND_LIMIT_WITH_PLACEHOLDER_PATTERN = Pattern
			.compile(""
					 + "(?ims)"
					 + ".+SKIP" + COMMENT_OR_WHITESPACE_GROUP + "+"
					 + "\\$" + COMMENT_OR_WHITESPACE_GROUP + "*(?:(?-i)skip)" + COMMENT_OR_WHITESPACE_GROUP + "+"
					 + "LIMIT" + COMMENT_OR_WHITESPACE_GROUP + "+"
					 + "\\$" + COMMENT_OR_WHITESPACE_GROUP + "*(?:(?-i)limit)"
					 + ".*");

	/**
	 * Used to evaluate the expression found while parsing the cypher template of this query against the actual parameters
	 * with the help of the formal parameters during the building of the {@link PreparedQuery}.
	 */
	private final SpelEvaluator spelEvaluator;

	/**
	 * An optional evaluator for a count query if such a query is present.
	 */
	private final Optional<SpelEvaluator> spelEvaluatorForCountQuery;

	/**
	 * Create a {@link StringBasedNeo4jQuery} for a query method that is annotated with {@link Query @Query}. The
	 * annotation is expected to have a value.
	 *
	 * @param neo4jOperations           the Neo4j operations
	 * @param mappingContext            a Neo4jMappingContext instance
	 * @param evaluationContextProvider a QueryMethodEvaluationContextProvider instance
	 * @param queryMethod               the query method
	 * @return A new instance of a String based Neo4j query.
	 */
	static StringBasedNeo4jQuery create(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
						QueryMethodEvaluationContextProvider evaluationContextProvider, Neo4jQueryMethod queryMethod,
						ProjectionFactory factory) {

		Query queryAnnotation = queryMethod.getQueryAnnotation()
				.orElseThrow(() -> new MappingException("Expected @Query annotation on the query method!"));

		boolean requiresCount = queryMethod.isPageQuery();
		boolean supportsCount = queryMethod.isSliceQuery();
		boolean requiresSkipAndLimit = queryMethod.isSliceQuery() || requiresCount;
		boolean countQueryPresent = StringUtils.hasText(queryAnnotation.countQuery());

		if (!countQueryPresent) {
			if (requiresCount) {
				throw new MappingException("Expected paging query method to have a count query!");
			}
			if (supportsCount) {
				Neo4jQuerySupport.REPOSITORY_QUERY_LOG.debug(() -> String.format(
						"You provided a string based query returning a slice for '%s.%s'. "
						+ "You might want to consider adding a count query if more slices than you expect are returned.",
						queryMethod.getRepositoryName(), queryMethod.getName()));
			}
		}

		String cypherTemplate = Optional.ofNullable(queryAnnotation.value()).filter(StringUtils::hasText)
				.orElseThrow(() -> new MappingException("Expected @Query annotation to have a value, but it did not."));

		if (requiresSkipAndLimit && !hasSkipAndLimitKeywordsAndPlaceholders(cypherTemplate)) {
			Neo4jQuerySupport.REPOSITORY_QUERY_LOG.warn(() ->
					String.format("The custom query %n%s%n"
								  + "for '%s.%s' is supposed to work with a page or slicing query but does not have the required "
								  + "parameter placeholders `$skip` and `$limit`.%n"
								  + "Be aware that those parameters are case sensitive and SDN uses the lower case variant.",
							cypherTemplate, queryMethod.getRepositoryName(), queryMethod.getName()));
		}

		return new StringBasedNeo4jQuery(neo4jOperations, mappingContext, evaluationContextProvider, queryMethod,
				cypherTemplate, Neo4jQueryType.fromDefinition(queryAnnotation), factory);
	}

	/**
	 * Create a {@link StringBasedNeo4jQuery} based on an explicit Cypher template.
	 *
	 * @param neo4jOperations           the Neo4j operations
	 * @param mappingContext            a Neo4jMappingContext instance
	 * @param evaluationContextProvider a QueryMethodEvaluationContextProvider instance
	 * @param queryMethod               the query method
	 * @param cypherTemplate            The template to use.
	 * @return A new instance of a String based Neo4j query.
	 */
	static StringBasedNeo4jQuery create(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			QueryMethodEvaluationContextProvider evaluationContextProvider, Neo4jQueryMethod queryMethod,
			String cypherTemplate, ProjectionFactory factory) {

		Assert.hasText(cypherTemplate, "Cannot create String based Neo4j query without a cypher template.");

		return new StringBasedNeo4jQuery(neo4jOperations, mappingContext, evaluationContextProvider, queryMethod,
				cypherTemplate, Neo4jQueryType.DEFAULT, factory);
	}

	private StringBasedNeo4jQuery(Neo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			QueryMethodEvaluationContextProvider evaluationContextProvider, Neo4jQueryMethod queryMethod,
			String cypherTemplate, Neo4jQueryType queryType, ProjectionFactory factory) {

		super(neo4jOperations, mappingContext, queryMethod, queryType, factory);

		Parameters<?, ?> methodParameters = queryMethod.getParameters();
		cypherTemplate = Neo4jSpelSupport.renderQueryIfExpressionOrReturnQuery(cypherTemplate, mappingContext, queryMethod.getEntityInformation(), SPEL_EXPRESSION_PARSER);
		this.spelEvaluator = new SpelEvaluator(
				evaluationContextProvider, methodParameters, SPEL_QUERY_CONTEXT.parse(cypherTemplate));
		this.spelEvaluatorForCountQuery = queryMethod.getQueryAnnotation()
				.map(Query::countQuery)
				.map(q -> Neo4jSpelSupport.renderQueryIfExpressionOrReturnQuery(q, mappingContext, queryMethod.getEntityInformation(), SPEL_EXPRESSION_PARSER))
				.map(countQuery -> new SpelEvaluator(evaluationContextProvider, methodParameters, SPEL_QUERY_CONTEXT.parse(countQuery)));
	}

	@Override
	protected <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType, Map<PropertyPath, Boolean> includedProperties,
			Neo4jParameterAccessor parameterAccessor, @Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction,
			UnaryOperator<Integer> limitModifier
	) {

		Map<String, Object> boundParameters = bindParameters(parameterAccessor, true, limitModifier);
		QueryContext queryContext = new QueryContext(
				queryMethod.getRepositoryName() + "." + queryMethod.getName(),
				spelEvaluator.getQueryString(),
				boundParameters
		);

		replaceLiteralsIn(queryContext);
		logWarningsIfNecessary(queryContext, parameterAccessor);

		return PreparedQuery.queryFor(returnedType)
				.withCypherQuery(queryContext.query)
				.withParameters(boundParameters)
				.usingMappingFunction(mappingFunction)
				.build();
	}

	Map<String, Object> bindParameters(Neo4jParameterAccessor parameterAccessor, boolean includePageableParameter,
			UnaryOperator<Integer> limitModifier) {

		final Parameters<?, ?> formalParameters = parameterAccessor.getParameters();
		Map<String, Object> resolvedParameters = new HashMap<>();

		// Values from the parameter accessor can only get converted after evaluation
		for (Entry<String, Object> evaluatedParam : spelEvaluator.evaluate(parameterAccessor.getValues()).entrySet()) {
			Object value = evaluatedParam.getValue();
			if (!(evaluatedParam.getValue() instanceof LiteralReplacement)) {
				Neo4jQuerySupport.logParameterIfNull(evaluatedParam.getKey(), value);
				value = super.convertParameter(evaluatedParam.getValue());
			}
			resolvedParameters.put(evaluatedParam.getKey(), value);
		}

		formalParameters.getBindableParameters().forEach(parameter -> {

			int index = parameter.getIndex();
			Object value = parameterAccessor.getBindableValue(index);
			Neo4jQuerySupport.logParameterIfNull(parameter.getName().orElseGet(() -> Integer.toString(index)), value);
			Object convertedValue = super.convertParameter(value);

			// Add the parameter under its name when possible
			parameter.getName().ifPresent(parameterName -> resolvedParameters.put(parameterName, convertedValue));
			// Always add under its index.
			resolvedParameters.put(Integer.toString(index), convertedValue);
		});

		if (formalParameters.hasPageableParameter() && includePageableParameter) {
			Pageable pageable = parameterAccessor.getPageable();
			resolvedParameters.put("limit", limitModifier.apply(pageable.getPageSize()));
			resolvedParameters.put("skip", pageable.getOffset());
		}

		return resolvedParameters;
	}

	@Override
	protected Optional<PreparedQuery<Long>> getCountQuery(Neo4jParameterAccessor parameterAccessor) {
		return spelEvaluatorForCountQuery.map(SpelEvaluator::getQueryString)
				.map(countQuery -> {
					Map<String, Object> boundParameters = bindParameters(parameterAccessor, false, UnaryOperator.identity());
					QueryContext queryContext = new QueryContext(
							queryMethod.getRepositoryName() + "." + queryMethod.getName(),
							countQuery,
							boundParameters
					);

					replaceLiteralsIn(queryContext);

					return PreparedQuery.queryFor(Long.class)
							.withCypherQuery(queryContext.query)
							.withParameters(boundParameters)
							.build();
				});
	}

	/**
	 * @param index                  position of this parameter placeholder
	 * @param originalSpelExpression Not used for configuring parameter names atm.
	 * @return A new parameter name for the given index.
	 */
	private static String parameterNameSource(int index, @SuppressWarnings("unused") String originalSpelExpression) {
		return "__SpEL__" + index;
	}

	/**
	 * @param originalPrefix The prefix passed to the replacement source is either ':' or '?', so that isn't usable for
	 *                       Cypher templates and therefore ignored.
	 * @param parameterName  name of the parameter
	 * @return The name of the parameter in its native Cypher form.
	 */
	private static String replacementSource(@SuppressWarnings("unused") String originalPrefix, String parameterName) {
		return "$" + parameterName;
	}

	static boolean hasSkipAndLimitKeywordsAndPlaceholders(String queryTemplate) {
		return SKIP_AND_LIMIT_WITH_PLACEHOLDER_PATTERN.matcher(queryTemplate).matches();
	}
}
