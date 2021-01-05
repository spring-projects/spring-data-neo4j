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
