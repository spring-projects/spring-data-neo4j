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

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;

public class ParameterizedQuery {
	private static final String PATTERN = "[:?]#\\{(#?[^}]+)}";
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();

	private final EvaluationContextProvider evaluationContextProvider;
	private final String queryString;
	private final Map<String, String> processedExpressions;

	private ParameterizedQuery(String queryString, Map<String, String> expressionParameters,
			EvaluationContextProvider evaluationContextProvider) {

		this.evaluationContextProvider = evaluationContextProvider;
		this.queryString = queryString;
		this.processedExpressions = expressionParameters;
	}

	public static ParameterizedQuery getParameterizedQuery(String queryString,
			EvaluationContextProvider evaluationContextProvider, PlaceholderSupplier supplier) {

		String processedQuery = queryString;
		Matcher matcher = Pattern.compile(PATTERN).matcher(queryString);
		Map<String, String> processedExpressions = new HashMap<>();

		int expressionRegexGroupIndex = 1;
		while (matcher.find()) {

			String expression = matcher.group(expressionRegexGroupIndex);
			if (processedExpressions.containsKey(expression)) {

				String decoratedPlaceholder = supplier.decoratePlaceholder(processedExpressions.get(expression));
				processedQuery = processedQuery.replaceFirst(PATTERN, decoratedPlaceholder);
			} else {

				String placeholder = supplier.nextPlaceholder();
				String decoratedPlaceholder = supplier.decoratePlaceholder(placeholder);

				processedExpressions.put(expression, placeholder);
				processedQuery = processedQuery.replaceFirst(PATTERN, decoratedPlaceholder);
			}
		}

		return new ParameterizedQuery(processedQuery, processedExpressions, evaluationContextProvider);
	}

	private static Object getSpElValue(EvaluationContext evaluationContext, String expression) {
		return PARSER.parseExpression(expression).getValue(evaluationContext, Object.class);
	}

	public Map<String, Object> resolveParameter(Parameters<?, ?> methodParameters, Object[] parameters,
			BiFunction<Parameters<?, ?>, Object[], Map<String, Object>> nativePlaceholderFunction) {

		EvaluationContext evaluationContext = evaluationContextProvider.getEvaluationContext(methodParameters, parameters);
		Map<String, Object> parameterValues = new HashMap<>(nativePlaceholderFunction.apply(methodParameters, parameters));

		for (Map.Entry<String, String> expression : processedExpressions.entrySet()) {
			parameterValues.put(expression.getValue(), getSpElValue(evaluationContext, expression.getKey()));
		}
		return parameterValues;
	}

	public String getQueryString() {
		return queryString;
	}

}
