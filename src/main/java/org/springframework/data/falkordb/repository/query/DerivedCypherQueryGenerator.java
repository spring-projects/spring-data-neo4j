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

import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBPersistentEntity;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * Query generator that creates Cypher queries from method names. Converts method names
 * like findByName to MATCH (n:Person) WHERE n.name = $name RETURN n.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class DerivedCypherQueryGenerator {

	private final DefaultFalkorDBPersistentEntity<?> entity;

	private final FalkorDBMappingContext mappingContext;

	private final PartTree partTree;

	/**
	 * Creates a new {@link DerivedCypherQueryGenerator}.
	 * @param tree the part tree representing the query structure
	 * @param entity the persistent entity
	 * @param mappingContext the mapping context
	 */
	public DerivedCypherQueryGenerator(PartTree tree, DefaultFalkorDBPersistentEntity<?> entity,
			FalkorDBMappingContext mappingContext) {
		this.partTree = tree;
		this.entity = entity;
		this.mappingContext = mappingContext;
	}

	/**
	 * Creates a Cypher query from the method name and parameters.
	 * @param sort the sort specification
	 * @param parameters the query parameters
	 * @return the generated Cypher query
	 */
	public CypherQuery createQuery(Sort sort, Object... parameters) {
		String primaryLabel = this.entity.getPrimaryLabel();
		StringBuilder cypher = new StringBuilder();

		cypher.append("MATCH (n:").append(primaryLabel).append(")");

		Map<String, Object> queryParameters = new HashMap<>();

		// Simple implementation - for now, we'll support basic queries
		// TODO: Parse PartTree to build WHERE conditions properly
		if (this.partTree.getParts().iterator().hasNext()) {
			cypher.append(" WHERE 1=1"); // Placeholder condition
			// In a full implementation, we would parse the PartTree here
		}

		cypher.append(" RETURN n");

		if (sort.isSorted()) {
			cypher.append(" ORDER BY ");
			sort.forEach(order -> {
				cypher.append("n.")
					.append(order.getProperty())
					.append(" ")
					.append(order.getDirection().name())
					.append(", ");
			});
			// Remove trailing comma and space
			cypher.setLength(cypher.length() - 2);
		}

		return new CypherQuery(cypher.toString(), queryParameters);
	}

}

/**
 * Represents a Cypher query with parameters.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
class CypherQuery {

	private final String query;

	private final Map<String, Object> parameters;

	/**
	 * Creates a new CypherQuery.
	 * @param query the Cypher query string
	 * @param parameters the query parameters
	 */
	CypherQuery(String query, Map<String, Object> parameters) {
		this.query = query;
		this.parameters = (parameters != null) ? parameters : new HashMap<>();
	}

	/**
	 * Gets the Cypher query string.
	 * @return the query string
	 */
	String getQuery() {
		return this.query;
	}

	/**
	 * Gets the query parameters.
	 * @return the parameters map
	 */
	Map<String, Object> getParameters() {
		return this.parameters;
	}

}

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

	CypherCondition(Part part, Object value, DefaultFalkorDBPersistentEntity<?> entity) {
		this.parameters = new HashMap<>();
		String paramName = "param" + (++paramCounter);
		String propertyName = part.getProperty().getSegment();

		this.fragment = buildCondition(part, propertyName, paramName);
		this.parameters.put(paramName, value);
	}

	private CypherCondition(String fragment, Map<String, Object> parameters) {
		this.fragment = fragment;
		this.parameters = parameters;
	}

	private String buildCondition(Part part, String propertyName, String paramName) {
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

	CypherCondition and(CypherCondition other) {
		Map<String, Object> combinedParams = new HashMap<>(this.parameters);
		combinedParams.putAll(other.parameters);
		String combinedFragment = "(" + this.fragment + " AND " + other.fragment + ")";
		return new CypherCondition(combinedFragment, combinedParams);
	}

	CypherCondition or(CypherCondition other) {
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
