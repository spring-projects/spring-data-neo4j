/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.ReactiveNeo4jOperations;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ValueExpressionDelegate;
import org.springframework.data.repository.query.ValueExpressionQueryRewriter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Implementation of {@link RepositoryQuery} for query methods annotated with
 * {@link Query @Query}. The flow to handle queries with SpEL parameters is as follows
 * <ol>
 * <li>Parse template as something that has SpEL-expressions in it</li>
 * <li>Replace the SpEL-expressions with Neo4j Statement template parameters</li>
 * <li>The parameters passed here _and_ the values that might have been computed during
 * SpEL-parsing</li>
 * </ol>
 * The main ingredient is a SpelEvaluator, that parses a template and replaces SpEL
 * expressions with real Neo4j parameters.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactiveStringBasedNeo4jQuery extends AbstractReactiveNeo4jQuery {

	private final ValueExpressionQueryRewriter.EvaluatingValueExpressionQueryRewriter queryRewriter;

	private final ValueExpressionQueryRewriter.QueryExpressionEvaluator parsedQuery;

	private ReactiveStringBasedNeo4jQuery(ReactiveNeo4jOperations neo4jOperations, Neo4jMappingContext mappingContext,
			ValueExpressionDelegate delegate, Neo4jQueryMethod queryMethod, String cypherTemplate,
			Neo4jQueryType queryType, ProjectionFactory factory) {

		super(neo4jOperations, mappingContext, queryMethod, queryType, factory);

		this.queryRewriter = createQueryRewriter(delegate);
		this.parsedQuery = this.queryRewriter.parse(cypherTemplate, queryMethod.getParameters());
	}

	/**
	 * Create a {@link ReactiveStringBasedNeo4jQuery} for a query method that is annotated
	 * with {@link Query @Query}. The annotation is expected to have a value.
	 * @param neo4jOperations reactive Neo4j operations
	 * @param mappingContext a Neo4jMappingContext instance
	 * @param delegate a ValueExpressionDelegate instance
	 * @param queryMethod the query method
	 * @param factory the projection factory to work with
	 * @return a new instance of a String based Neo4j query
	 */
	static ReactiveStringBasedNeo4jQuery create(ReactiveNeo4jOperations neo4jOperations,
			Neo4jMappingContext mappingContext, ValueExpressionDelegate delegate, Neo4jQueryMethod queryMethod,
			ProjectionFactory factory) {

		Query queryAnnotation = queryMethod.getQueryAnnotation()
			.orElseThrow(() -> new MappingException("Expected @Query annotation on the query method"));

		String cypherTemplate = Optional.ofNullable(queryAnnotation.value())
			.filter(StringUtils::hasText)
			.orElseThrow(() -> new MappingException("Expected @Query annotation to have a value, but it did not"));

		return new ReactiveStringBasedNeo4jQuery(neo4jOperations, mappingContext, delegate, queryMethod, cypherTemplate,
				Neo4jQueryType.fromDefinition(queryAnnotation), factory);
	}

	/**
	 * Create a {@link ReactiveStringBasedNeo4jQuery} based on an explicit Cypher
	 * template.
	 * @param neo4jOperations reactive Neo4j operations
	 * @param mappingContext a Neo4jMappingContext instance
	 * @param delegate a ValueExpressionDelegate instance
	 * @param queryMethod the query method
	 * @param cypherTemplate the template to use.
	 * @param factory the projection factory to work with
	 * @return a new instance of a String based Neo4j query.
	 */
	static ReactiveStringBasedNeo4jQuery create(ReactiveNeo4jOperations neo4jOperations,
			Neo4jMappingContext mappingContext, ValueExpressionDelegate delegate, Neo4jQueryMethod queryMethod,
			String cypherTemplate, ProjectionFactory factory) {

		Assert.hasText(cypherTemplate, "Cannot create String based Neo4j query without a cypher template");

		return new ReactiveStringBasedNeo4jQuery(neo4jOperations, mappingContext, delegate, queryMethod, cypherTemplate,
				Neo4jQueryType.DEFAULT, factory);
	}

	static ValueExpressionQueryRewriter.EvaluatingValueExpressionQueryRewriter createQueryRewriter(
			ValueExpressionDelegate delegate) {
		return ValueExpressionQueryRewriter.of(delegate, StringBasedNeo4jQuery::parameterNameSource,
				StringBasedNeo4jQuery::replacementSource);
	}

	@Override
	protected <T extends Object> PreparedQuery<T> prepareQuery(Class<T> returnedType,
			Collection<PropertyFilter.ProjectedPath> includedProperties, Neo4jParameterAccessor parameterAccessor,
			@Nullable Neo4jQueryType queryType, Supplier<BiFunction<TypeSystem, MapAccessor, ?>> mappingFunction,
			UnaryOperator<Integer> limitModifier) {

		Map<String, Object> boundParameters = bindParameters(parameterAccessor);
		QueryContext queryContext = new QueryContext(
				this.queryMethod.getRepositoryName() + "." + this.queryMethod.getName(),
				this.parsedQuery.getQueryString(), boundParameters);

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
		for (Map.Entry<String, Object> evaluatedParam : this.parsedQuery.evaluate(parameterAccessor.getValues())
			.entrySet()) {
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

}
