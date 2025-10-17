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
import java.util.Map;

import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.lang.Nullable;

/**
 * {@link RepositoryQuery} implementation that executes custom Cypher queries
 * defined via the {@link Query} annotation.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
class StringBasedFalkorDBQuery implements RepositoryQuery {

	private final FalkorDBQueryMethod queryMethod;
	private final FalkorDBOperations operations;

	/**
	 * Creates a new {@link StringBasedFalkorDBQuery}.
	 * @param queryMethod must not be {@literal null}.
	 * @param operations must not be {@literal null}.
	 */
	StringBasedFalkorDBQuery(FalkorDBQueryMethod queryMethod, FalkorDBOperations operations) {
		this.queryMethod = queryMethod;
		this.operations = operations;
	}

	@Override
	public Object execute(Object[] parameters) {
		
		String query = queryMethod.getAnnotatedQuery();
		if (query == null) {
			throw new IllegalStateException("No query defined for method " + queryMethod.getName());
		}

		Map<String, Object> parameterMap = createParameterMap(parameters);
		
		ResultProcessor processor = queryMethod.getResultProcessor().withDynamicProjection(parameters);
		ReturnedType returnedType = processor.getReturnedType();

		if (queryMethod.isCountQuery()) {
			Long count = operations.count(query, parameterMap);
			return processor.processResult(count);
		}

		if (queryMethod.isExistsQuery()) {
			Boolean exists = operations.exists(query, parameterMap);
			return processor.processResult(exists);
		}

		if (queryMethod.isCollectionQuery()) {
			return processor.processResult(
				operations.findAll(returnedType.getDomainType(), query, parameterMap)
			);
		}

		// Single result query
		Object result = operations.findOne(returnedType.getDomainType(), query, parameterMap);
		return processor.processResult(result);
	}

	@Override
	public FalkorDBQueryMethod getQueryMethod() {
		return queryMethod;
	}

	/**
	 * Creates a parameter map from the method arguments.
	 * @param parameters the method arguments
	 * @return the parameter map
	 */
	private Map<String, Object> createParameterMap(Object[] parameters) {
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