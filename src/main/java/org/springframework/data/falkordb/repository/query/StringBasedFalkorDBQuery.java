/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
