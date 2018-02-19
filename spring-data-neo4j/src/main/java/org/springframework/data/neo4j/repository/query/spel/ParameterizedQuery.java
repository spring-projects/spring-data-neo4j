/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.query.spel;

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.SpelQueryContextFactory;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class ParameterizedQuery {
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

//	private final EvaluationContextProvider evaluationContextProvider;
//	private final String queryString;
//	private final Map<String, String> processedExpressions;
	private final SpelQueryContextFactory.SpelQueryContext.SpelExtractor extractor;

//	private ParameterizedQuery(String queryString, Map<String, String> expressionParameters,
//							   EvaluationContextProvider evaluationContextProvider) {
//
//		this.evaluationContextProvider = evaluationContextProvider;
//		this.queryString = queryString;
//		this.processedExpressions = expressionParameters;
//	}

	public ParameterizedQuery(SpelQueryContextFactory.SpelQueryContext.SpelExtractor extractor) {

		this.extractor = extractor;
	}

	public static ParameterizedQuery getParameterizedQuery(String queryString,
														   EvaluationContextProvider evaluationContextProvider, PlaceholderSupplier supplier) {

		Neo4jQueryPlaceholderSupplier supplier1 = new Neo4jQueryPlaceholderSupplier();

		SpelQueryContextFactory.SpelQueryContext spelQueryContext = new SpelQueryContextFactory(evaluationContextProvider).createSpelQueryContext((index, prefix) -> supplier1.parameterName(index), (prefix, name) -> supplier1.decoratePlaceholder(name));

		SpelQueryContextFactory.SpelQueryContext.SpelExtractor extractor = spelQueryContext.parse(queryString);

		return new ParameterizedQuery(extractor);
	}

	private static Object getSpElValue(EvaluationContext evaluationContext, String expression) {
		return PARSER.parseExpression(expression).getValue(evaluationContext, Object.class);
	}

	public Map<String, Object> resolveParameter(Parameters<?, ?> methodParameters, Object[] parameters,
												BiFunction<Parameters<?, ?>, Object[], Map<String, Object>> nativePlaceholderFunction) {

		Map<String, Object> evaluate = extractor.createEvaluator(methodParameters).evaluate(parameters);
		evaluate.putAll(nativePlaceholderFunction.apply(methodParameters, parameters));
		return evaluate;

//		EvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(methodParameters, parameters);
//		Map<String, Object> parameterValues = new HashMap<>(nativePlaceholderFunction.apply(methodParameters, parameters));
//
//		for (Map.Entry<String, String> expression : processedExpressions.entrySet()) {
//			parameterValues.put(expression.getKey(), getSpElValue(evaluationContext, expression.getValue()));
//		}
//		return parameterValues;
	}

	public String getQueryString() {
		return extractor.query();
	}

}
