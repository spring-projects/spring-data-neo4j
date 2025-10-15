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

import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBPersistentEntity;
import org.springframework.data.repository.query.parser.Part;

/**
 * Represents a condition in a Cypher query.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
class CypherCondition {

	private final String fragment;

	private final Map<String, Object> parameters;

	private static int paramCounter = 0;

	CypherCondition(final Part part, final Object value, final DefaultFalkorDBPersistentEntity<?> entity) {
		this.parameters = new HashMap<>();
		String paramName = "param" + (++paramCounter);
		String propertyName = part.getProperty().getSegment();

		this.fragment = buildCondition(part, propertyName, paramName);
		this.parameters.put(paramName, value);
	}

	private CypherCondition(final String fragment, final Map<String, Object> parameters) {
		this.fragment = fragment;
		this.parameters = parameters;
	}

	private String buildCondition(final Part part, final String propertyName, final String paramName) {
		switch (part.getType()) {
			case SIMPLE_PROPERTY:
				return "n." + propertyName + " = $" + paramName;
			case GREATER_THAN:
				return "n." + propertyName + " > $" + paramName;
			case GREATER_THAN_EQUAL:
				return "n." + propertyName + " >= $" + paramName;
			case LESS_THAN:
				return "n." + propertyName + " < $" + paramName;
			case LESS_THAN_EQUAL:
				return "n." + propertyName + " <= $" + paramName;
			case LIKE:
				return "n." + propertyName + " CONTAINS $" + paramName;
			case CONTAINING:
				return "n." + propertyName + " CONTAINS $" + paramName;
			case STARTING_WITH:
				return "n." + propertyName + " STARTS WITH $" + paramName;
			case ENDING_WITH:
				return "n." + propertyName + " ENDS WITH $" + paramName;
			case IS_NULL:
				return "n." + propertyName + " IS NULL";
			case IS_NOT_NULL:
				return "n." + propertyName + " IS NOT NULL";
			case BETWEEN:
				// This would need special handling for two parameters
				return "n." + propertyName + " >= $" + paramName + " AND n." + propertyName + " <= $" + paramName;
			default:
				return "n." + propertyName + " = $" + paramName;
		}
	}

	CypherCondition and(final CypherCondition other) {
		Map<String, Object> combinedParams = new HashMap<>(this.parameters);
		combinedParams.putAll(other.parameters);
		String combinedFragment = "(" + this.fragment + " AND " + other.fragment + ")";
		return new CypherCondition(combinedFragment, combinedParams);
	}

	CypherCondition or(final CypherCondition other) {
		Map<String, Object> combinedParams = new HashMap<>(this.parameters);
		combinedParams.putAll(other.parameters);
		String combinedFragment = "(" + this.fragment + " OR " + other.fragment + ")";
		return new CypherCondition(combinedFragment, combinedParams);
	}

	String getCypherFragment() {
		return this.fragment;
	}

	Map<String, Object> getParameters() {
		return this.parameters;
	}

}
