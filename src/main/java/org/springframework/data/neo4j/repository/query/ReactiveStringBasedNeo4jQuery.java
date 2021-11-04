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
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.SpelEvaluator;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.data.repository.query.SpelQueryContext.SpelExtractor;
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
final class ReactiveStringBasedNeo4jQuery extends AbstractReactiveNeo4jQuery {

	/**
	 * Used for extracting SpEL expressions inside Cypher query templates.
	 */
	static final SpelQueryContext SPEL_QUERY_CONTEXT = SpelQueryContext
			.of(ReactiveStringBasedNeo4jQuery::parameterNameSource, ReactiveStringBasedNeo4jQuery::replacementSource);

	/**
	 * Used to evaluate the expression found while parsing the cypher template of this query against the actual parameters
	 * with the help of the formal parameters during the building of the {@link PreparedQuery}.
	 */
	private final SpelEvaluator spelEvaluator;

	/**
	 * Create a {@link ReactiveStringBasedNeo4jQuery} for a query method that is annotated with {@link Query @Query}. The
	 * annotation is expected to have a value.
	 *
	 * @param neo4jOperations reactive Neo4j operations
	 * @param mappingContext a Neo4jMappingContext instance
	 * @param evaluationContextProvider a QueryMethodEvaluationContextProvider instance
	 * @param queryMethod the query method
	 * @return A new instance of a String based Neo4j query.
	 */
	static ReactiveStringBasedNeo4jQuery create(ReactiveNeo4jOperations neo4jOperations,
			Neo4jMappingContext mappingContext, QueryMethodEvaluationContextProvider evaluationContextProvider,
			Neo4jQueryMethod queryMethod, ProjectionFactory factory) {

		Query queryAnnotation = queryMethod.getQueryAnnotation()
				.orElseThrow(() -> new MappingException("Expected @Query annotation on the query method!"));

		String cypherTemplate = Optional.ofNullable(queryAnnotation.value()).filter(StringUtils::hasText)
				.orElseThrow(() -> new MappingException("Expected @Query annotation to have a value, but it did not."));

		return new ReactiveStringBasedNeo4jQuery(neo4jOperations, mappingContext, evaluationContextProvider, queryMethod,
				cypherTemplate, Neo4jQueryType.fromDefinition(queryAnnotation), factory);
	}

	/**
	 * Create a {@link ReactiveStringBasedNeo4jQuery} based on an explicit Cypher template.
	 *
	 * @param neo4jOperations reactive Neo4j operations
	 * @param mappingContext a Neo4jMappingContext instance
	 * @param evaluationContextProvider a QueryMethodEvaluationContextProvider instance
	 * @param queryMethod the query method
	 * @param cypherTemplate The template to use.
	 * @return A new instance of a String based Neo4j query.
	 */
	static ReactiveStringBasedNeo4jQuery create(ReactiveNeo4jOperations neo4jOperations,
			Neo4jMappingContext mappingContext, QueryMethodEvaluationContextProvider evaluationContextProvider,
			Neo4jQueryMethod queryMethod, String cypherTemplate, ProjectionFactory factory) {

		Assert.hasText(cypherTemplate, "Cannot create String based Neo4j query without a cypher template.");

		return new ReactiveStringBasedNeo4jQuery(neo4jOperations, mappingContext, evaluationContextProvider, queryMethod,
				cypherTemplate, Neo4jQueryType.DEFAULT, factory);
	}

	private ReactiveStringBasedNeo4jQuery(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			QueryMethodEvaluationContextProvider evaluationContextProvider, Neo4jQueryMethod queryMethod,
			String cypherTemplate, Neo4jQueryType queryType, ProjectionFactory factory) {

		super(neo4jOperations, mappingContext, queryMethod, queryType, factory);

		cypherTemplate = Neo4jSpelSupport.renderQueryIfExpressionOrReturnQuery(cypherTemplate, mappingContext, queryMethod.getEntityInformation(), SPEL_EXPRESSION_PARSER);
		SpelExtractor spelExtractor = SPEL_QUERY_CONTEXT.parse(cypherTemplate);
		this.spelEvaluator = new SpelEvaluator(evaluationContextProvider, queryMethod.getParameters(), spelExtractor);
	}

	@Override
	protected <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType, Map<PropertyPath, Boolean> includedProperties,
			Neo4jParameterAccessor parameterAccessor, @Nullable Neo4jQueryType queryType,
			@Nullable Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction) {

		Map<String, Object> boundParameters = bindParameters(parameterAccessor);
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

	Map<String, Object> bindParameters(Neo4jParameterAccessor parameterAccessor) {

		final Parameters<?, ?> formalParameters = parameterAccessor.getParameters();
		Map<String, Object> resolvedParameters = new HashMap<>();

		// Values from the parameter accessor can only get converted after evaluation
		for (Map.Entry<String, Object> evaluatedParam : spelEvaluator.evaluate(parameterAccessor.getValues()).entrySet()) {
			Object value = evaluatedParam.getValue();
			if (!(evaluatedParam.getValue() instanceof Neo4jSpelSupport.LiteralReplacement)) {
				Neo4jQuerySupport.logParameterIfNull(evaluatedParam.getKey(), value);
				value = super.convertParameter(evaluatedParam.getValue());
			}
			resolvedParameters.put(evaluatedParam.getKey(), value);
		}

		formalParameters.stream().filter(Parameter::isBindable).forEach(parameter -> {

			int index = parameter.getIndex();
			Object value = parameterAccessor.getBindableValue(index);
			Neo4jQuerySupport.logParameterIfNull(parameter.getName().orElseGet(() -> Integer.toString(index)), value);
			Object convertedValue = super.convertParameter(value);

			// Add the parameter under its name when possible
			parameter.getName().ifPresent(parameterName -> resolvedParameters.put(parameterName, convertedValue));
			// Always add under its index.
			resolvedParameters.put(Integer.toString(index), convertedValue);
		});

		return resolvedParameters;
	}

	/**
	 * @param index position of this parameter placeholder
	 * @param originalSpelExpression Not used for configuring parameter names atm.
	 * @return A new parameter name for the given index.
	 */
	private static String parameterNameSource(int index, @SuppressWarnings("unused") String originalSpelExpression) {
		return "__SpEL__" + index;
	}

	/**
	 * @param originalPrefix The prefix passed to the replacement source is either ':' or '?', so that isn't usable for
	 *          Cypher templates and therefore ignored.
	 * @param parameterName name of the parameter
	 * @return The name of the parameter in its native Cypher form.
	 */
	private static String replacementSource(@SuppressWarnings("unused") String originalPrefix, String parameterName) {
		return "$" + parameterName;
	}
}
