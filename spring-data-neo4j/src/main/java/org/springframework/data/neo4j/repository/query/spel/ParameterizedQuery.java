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

import java.util.Map;
import java.util.function.BiFunction;

import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.SpelEvaluator;
import org.springframework.data.repository.query.SpelQueryContext;
import org.springframework.data.repository.query.SpelQueryContext.EvaluatingSpelQueryContext;

public class ParameterizedQuery {

	private final Parameters<?, ?> methodParameters;
	private final SpelEvaluator spelEvaluator;

	private ParameterizedQuery(Parameters<?, ?> methodParameters, SpelEvaluator spelEvaluator) {
		this.methodParameters = methodParameters;
		this.spelEvaluator = spelEvaluator;
	}

	public static ParameterizedQuery getParameterizedQuery(String queryString, Parameters<?, ?> methodParameters,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {

		Neo4jQueryPlaceholderSupplier supplier = new Neo4jQueryPlaceholderSupplier();

		EvaluatingSpelQueryContext context = SpelQueryContext.of( //
				(index, prefix) -> supplier.parameterName(index), //
				(prefix, name) -> supplier.decoratePlaceholder(name)) //
				.withEvaluationContextProvider(evaluationContextProvider);

		SpelEvaluator evaluator = context.parse(queryString, methodParameters);
		return new ParameterizedQuery(methodParameters, evaluator);
	}

	public Map<String, Object> resolveParameter(Object[] parameters,
			BiFunction<Parameters<?, ?>, Object[], Map<String, Object>> nativePlaceholderFunction) {

		Map<String, Object> parameterValues = spelEvaluator.evaluate(parameters);
		Map<String, Object> nativeParameterValues = nativePlaceholderFunction.apply(methodParameters, parameters);
		parameterValues.putAll(nativeParameterValues);
		return parameterValues;
	}

	public String getQueryString() {
		return spelEvaluator.getQueryString();
	}

}
