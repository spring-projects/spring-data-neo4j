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

import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.SpelEvaluator;
import org.springframework.data.repository.query.parser.SpelQueryContext;

public class ParameterizedQuery {
	private final Parameters<?, ?> methodParameters;
	private final SpelQueryContext.SpelExtractor extractor;
	private final SpelEvaluator spelEvaluator;

	private ParameterizedQuery(Parameters<?, ?> methodParameters, SpelQueryContext.SpelExtractor extractor,
			SpelEvaluator spelEvaluator) {
		this.methodParameters = methodParameters;
		this.extractor = extractor;
		this.spelEvaluator = spelEvaluator;
	}

	public static ParameterizedQuery getParameterizedQuery(String queryString, Parameters<?, ?> methodParameters,
			EvaluationContextProvider evaluationContextProvider) {

		Neo4jQueryPlaceholderSupplier supplier = new Neo4jQueryPlaceholderSupplier();

		SpelQueryContext spElQueryContext = new SpelQueryContext((index, prefix) -> supplier.parameterName(index),
				(prefix, name) -> supplier.decoratePlaceholder(name));

		SpelQueryContext.SpelExtractor extractor = spElQueryContext.parse(queryString);
		SpelEvaluator spelEvaluator = new SpelEvaluator(evaluationContextProvider, methodParameters,
				extractor.parameterNameToSpelMap());
		return new ParameterizedQuery(methodParameters, extractor, spelEvaluator);
	}

	public Map<String, Object> resolveParameter(Object[] parameters,
			BiFunction<Parameters<?, ?>, Object[], Map<String, Object>> nativePlaceholderFunction) {

		Map<String, Object> parameterValues = spelEvaluator.evaluate(parameters);
		Map<String, Object> nativeParameterValues = nativePlaceholderFunction.apply(methodParameters, parameters);
		parameterValues.putAll(nativeParameterValues);
		return parameterValues;
	}

	public String getQueryString() {
		return extractor.query();
	}

}
