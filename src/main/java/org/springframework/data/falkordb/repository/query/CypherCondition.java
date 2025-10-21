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

	/**
	 * The cypher fragment for this condition.
	 */
	private final String fragment;

	/**
	 * The parameters for this condition.
	 */
	private final Map<String, Object> parameters;

	/**
	 * Parameter counter for unique parameter names.
	 */
	private static int paramCounter = 0;

	/**
	 * Creates a new condition from a query part.
	 * @param part the query part
	 * @param value the parameter value
	 * @param entity the entity information
	 */
	CypherCondition(final Part part, final Object value, final DefaultFalkorDBPersistentEntity<?> entity) {
		this.parameters = new HashMap<>();
		String paramName = "param" + (++paramCounter);
		String propertyName = part.getProperty().getSegment();

		this.fragment = buildCondition(part, propertyName, paramName);
		this.parameters.put(paramName, value);
	}

	/**
	 * Creates a condition with explicit fragment and parameters.
	 * @param conditionFragment the cypher fragment
	 * @param conditionParams the parameters
	 */
	private CypherCondition(final String conditionFragment, final Map<String, Object> conditionParams) {
		this.fragment = conditionFragment;
		this.parameters = conditionParams;
	}

	/**
	 * Builds a cypher condition from a query part.
	 * @param part the query part
	 * @param propertyName the property name
	 * @param paramName the parameter name
	 * @return the cypher condition fragment
	 */
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

	/**
	 * Combines this condition with another using AND logic.
	 * @param other the other condition
	 * @return the combined condition
	 */
	CypherCondition and(final CypherCondition other) {
		Map<String, Object> combinedParams = new HashMap<>(this.parameters);
		combinedParams.putAll(other.parameters);
		String combinedFragment = "(" + this.fragment + " AND " + other.fragment + ")";
		return new CypherCondition(combinedFragment, combinedParams);
	}

	/**
	 * Combines this condition with another using OR logic.
	 * @param other the other condition
	 * @return the combined condition
	 */
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
