/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.falkordb.core.FalkorDBOperations;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.util.StringUtils;

/**
 * {@link RepositoryQuery} implementation that executes custom Cypher queries defined via
 * the {@link Query} annotation.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class StringBasedFalkorDBQuery implements RepositoryQuery {

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
	public StringBasedFalkorDBQuery(final FalkorDBQueryMethod method, final FalkorDBOperations falkorDBOperations) {
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

		ResultProcessor processor = queryMethod.getResultProcessor();
		ReturnedType returnedType = processor.getReturnedType();
		Class<?> returnType = returnedType.getReturnedType();

		if (queryMethod.isCountQuery()) {
			// For count queries, execute the query and return the count
			return processor.processResult(queryForScalar(query, parameterMap, Long.class));
		}

		if (queryMethod.isExistsQuery()) {
			// For exists queries, execute the query and return the boolean result
			return processor.processResult(queryForScalar(query, parameterMap, Boolean.class));
		}

		// Check if return type is a scalar type or Map
		if (queryMethod.isCollectionQuery()) {
			if (isScalarType(returnType) || Map.class.isAssignableFrom(returnType)) {
				// For scalar or Map collections, query as raw Maps and extract values
				return processor.processResult(queryForMaps(query, parameterMap));
			}
			else {
				// For entity collections, use normal entity mapping
				return processor.processResult(operations.query(query, parameterMap, returnedType.getDomainType()));
			}
		}

		// Single result query
		if (isScalarType(returnType)) {
			// For scalar types, extract the first column value
			return processor.processResult(queryForScalar(query, parameterMap, returnType));
		}
		else if (Map.class.isAssignableFrom(returnType)) {
			// For Map types, return as raw map
			List<Map<String, Object>> maps = queryForMaps(query, parameterMap);
			return processor.processResult(maps.isEmpty() ? null : maps.get(0));
		}
		else {
			// For entity types, use normal entity mapping
			Optional<?> result = operations.queryForObject(query, parameterMap, returnedType.getDomainType());
			return processor.processResult(result.orElse(null));
		}
	}

	public FalkorDBQueryMethod getQueryMethod() {
		return queryMethod;
	}

	/**
	 * Checks if the given class is a scalar type (primitive wrapper or String).
	 */
	private boolean isScalarType(Class<?> type) {
		return type.isPrimitive() ||
				Number.class.isAssignableFrom(type) ||
				Boolean.class.equals(type) ||
				String.class.equals(type);
	}

	/**
	 * Queries for scalar values (single column result).
	 */
	private <T> T queryForScalar(String query, Map<String, Object> parameters, Class<T> targetType) {
		return operations.query(query, parameters, result -> {
			for (org.springframework.data.falkordb.core.FalkorDBClient.Record record : result.records()) {
				// Get the first value from the record
				if (record.size() > 0) {
					Object value = record.get(0);
					return convertValue(value, targetType);
				}
			}
			return null;
		});
	}

	/**
	 * Queries and returns results as raw Maps.
	 */
	private List<Map<String, Object>> queryForMaps(String query, Map<String, Object> parameters) {
		return operations.query(query, parameters, result -> {
			List<Map<String, Object>> results = new ArrayList<>();
			for (org.springframework.data.falkordb.core.FalkorDBClient.Record record : result.records()) {
				Map<String, Object> map = new HashMap<>();
				for (String key : record.keys()) {
					map.put(key, record.get(key));
				}
				results.add(map);
			}
			return results;
		});
	}

	/**
	 * Converts a value to the target type.
	 */
	@SuppressWarnings("unchecked")
	private <T> T convertValue(Object value, Class<T> targetType) {
		if (value == null) {
			return null;
		}
		
		if (targetType.isInstance(value)) {
			return (T) value;
		}
		
		// Handle Number conversions
		if (value instanceof Number && Number.class.isAssignableFrom(targetType)) {
			Number num = (Number) value;
			if (targetType.equals(Long.class) || targetType.equals(long.class)) {
				return (T) Long.valueOf(num.longValue());
			}
			else if (targetType.equals(Integer.class) || targetType.equals(int.class)) {
				return (T) Integer.valueOf(num.intValue());
			}
			else if (targetType.equals(Double.class) || targetType.equals(double.class)) {
				return (T) Double.valueOf(num.doubleValue());
			}
			else if (targetType.equals(Float.class) || targetType.equals(float.class)) {
				return (T) Float.valueOf(num.floatValue());
			}
		}
		
		return (T) value;
	}

	/**
	 * Creates a parameter map from the method arguments.
	 * @param parameters the method arguments
	 * @return the parameter map
	 */
	private Map<String, Object> createParameterMap(final Object[] parameters) {
		Map<String, Object> parameterMap = new HashMap<>();

		if (parameters == null || parameters.length == 0) {
			return parameterMap;
		}

		// Add named parameters from @Param annotations
		java.lang.reflect.Parameter[] methodParameters = queryMethod.getMethod().getParameters();
		for (int i = 0; i < methodParameters.length; i++) {
			Param paramAnnotation = AnnotationUtils.findAnnotation(methodParameters[i], Param.class);
			if (paramAnnotation != null && StringUtils.hasText(paramAnnotation.value())) {
				parameterMap.put(paramAnnotation.value(), parameters[i]);
			}
			else {
				// Fallback to indexed parameters only if no @Param annotation
				parameterMap.put(String.valueOf(i), parameters[i]);
			}
		}

		return parameterMap;
	}

}
