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
package org.springframework.data.falkordb.repository.query;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;

/**
 * {@link RepositoryQuery} implementation that executes custom Cypher queries defined via
 * the {@link Query} annotation.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
class StringBasedFalkorDBQuery implements RepositoryQuery {

	/**
	 * The query method.
	 */
	private final FalkorDBQueryMethod queryMethod;

	/**
	 * The FalkorDB operations.
	 */
	private final FalkorDBOperations operations;

	/**
	 * Creates a new {@link StringBasedFalkorDBQuery}.
	 * @param method must not be {@literal null}.
	 * @param falkorDBOperations must not be {@literal null}.
	 */
	StringBasedFalkorDBQuery(final FalkorDBQueryMethod method, final FalkorDBOperations falkorDBOperations) {
		this.queryMethod = method;
		this.operations = falkorDBOperations;
	}

	@Override
	public Object execute(final Object[] parameters) {

		String query = queryMethod.getAnnotatedQuery();
		if (query == null) {
			throw new IllegalStateException("No query defined for method " + queryMethod.getName());
		}

		Map<String, Object> parameterMap = createParameterMap(parameters);

		// Create a simple parameter accessor - for now we'll handle this
		// differently
		// ResultProcessor processor =
		// queryMethod.getResultProcessor().withDynamicProjection(parameters);
		ResultProcessor processor = queryMethod.getResultProcessor();
		ReturnedType returnedType = processor.getReturnedType();

		if (queryMethod.isCountQuery()) {
			// For count queries, execute the query and return the count
			List<Long> results = operations.query(query, parameterMap, Long.class);
			Long count = results.isEmpty() ? 0L : results.get(0);
			return processor.processResult(count);
		}

		if (queryMethod.isExistsQuery()) {
			// For exists queries, execute the query and return the boolean
			// result
			List<Boolean> results = operations.query(query, parameterMap, Boolean.class);
			Boolean exists = results.isEmpty() ? false : results.get(0);
			return processor.processResult(exists);
		}

		if (queryMethod.isCollectionQuery()) {
			return processor.processResult(operations.query(query, parameterMap, returnedType.getDomainType()));
		}

		// Single result query
		Optional<?> result = operations.queryForObject(query, parameterMap, returnedType.getDomainType());
		return processor.processResult(result.orElse(null));
	}

	public FalkorDBQueryMethod getQueryMethod() {
		return queryMethod;
	}

	/**
	 * Creates a parameter map from the method arguments.
	 * @param parameters the method arguments
	 * @return the parameter map
	 */
	private Map<String, Object> createParameterMap(final Object[] parameters) {
		Map<String, Object> parameterMap = new HashMap<>();

		// Add indexed parameters ($0, $1, ...)
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				parameterMap.put(String.valueOf(i), parameters[i]);
			}
		}

		// Add named parameters if available
		// This would typically involve analyzing @Param annotations
		// For now, we'll keep it simple with indexed parameters

		return parameterMap;
	}

}
