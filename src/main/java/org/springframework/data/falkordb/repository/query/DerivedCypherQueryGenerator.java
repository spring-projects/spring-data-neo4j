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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

	/**
	 * The persistent entity.
	 */
	private final DefaultFalkorDBPersistentEntity<?> entity;

	/**
	 * The mapping context.
	 */
	private final FalkorDBMappingContext mappingContext;

	/**
	 * The parsed part tree.
	 */
	private final PartTree partTree;

	/**
	 * Creates a new {@link DerivedCypherQueryGenerator}.
	 * @param tree the part tree representing the query structure
	 * @param persistentEntity the persistent entity
	 * @param context the mapping context
	 */
	public DerivedCypherQueryGenerator(final PartTree tree, final DefaultFalkorDBPersistentEntity<?> persistentEntity,
			final FalkorDBMappingContext context) {
		this.partTree = tree;
		this.entity = persistentEntity;
		this.mappingContext = context;
	}

	/**
	 * Creates a Cypher query from the method name and parameters.
	 * @param sort the sort specification
	 * @param parameters the query parameters
	 * @return the generated Cypher query
	 */
	public CypherQuery createQuery(final Sort sort, final Object... parameters) {
		String primaryLabel = this.entity.getPrimaryLabel();
		StringBuilder cypher = new StringBuilder();

		cypher.append("MATCH (n:").append(primaryLabel).append(")");

		Map<String, Object> queryParameters = new HashMap<>();
		AtomicInteger parameterIndex = new AtomicInteger(0);

		// Build WHERE clause from PartTree
		if (this.partTree.getParts().iterator().hasNext()) {
			cypher.append(" WHERE ");
			Iterator<PartTree.OrPart> orParts = this.partTree.iterator();
			boolean firstOr = true;

			while (orParts.hasNext()) {
				if (!firstOr) {
					cypher.append(" OR ");
				}
				firstOr = false;

				PartTree.OrPart orPart = orParts.next();
				Iterator<Part> parts = orPart.iterator();
				boolean firstAnd = true;

				while (parts.hasNext()) {
					if (!firstAnd) {
						cypher.append(" AND ");
					}
					firstAnd = false;

					Part part = parts.next();
					appendCondition(cypher, part, queryParameters, parameters, parameterIndex);
				}
			}
		}

		// Handle count query
		if (this.partTree.isCountProjection()) {
			cypher.append(" RETURN count(n)");
		}
		// Handle exists query
		else if (this.partTree.isExistsProjection()) {
			cypher.append(" RETURN count(n) > 0");
		}
		// Handle delete query
		else if (this.partTree.isDelete()) {
			cypher.append(" DELETE n");
		}
		// Regular select query
		else {
			cypher.append(" RETURN n");
		}

		// Add ORDER BY clause
		if (sort.isSorted() && !this.partTree.isDelete()) {
			cypher.append(" ORDER BY ");
			boolean first = true;
			for (Sort.Order order : sort) {
				if (!first) {
					cypher.append(", ");
				}
				first = false;
				cypher.append("n.")
					.append(order.getProperty())
					.append(" ")
					.append(order.getDirection().name());
			}
		}

		// Add LIMIT clause for top/first queries
		if (this.partTree.isLimiting()) {
			Integer maxResults = this.partTree.getMaxResults();
			cypher.append(" LIMIT ").append(maxResults != null ? maxResults : 1);
		}

		return new CypherQuery(cypher.toString(), queryParameters);
	}

	/**
	 * Appends a condition to the Cypher query based on the Part.
	 * @param cypher the query builder
	 * @param part the part to process
	 * @param queryParameters the parameter map
	 * @param parameters the method parameters
	 * @param parameterIndex the current parameter index
	 */
	private void appendCondition(StringBuilder cypher, Part part, Map<String, Object> queryParameters,
			Object[] parameters, AtomicInteger parameterIndex) {

		String property = part.getProperty().toDotPath();
		String paramName = "param" + parameterIndex.getAndIncrement();

		switch (part.getType()) {
			case SIMPLE_PROPERTY:
				cypher.append("n.").append(property).append(" = $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case NEGATING_SIMPLE_PROPERTY:
				cypher.append("n.").append(property).append(" <> $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case GREATER_THAN:
				cypher.append("n.").append(property).append(" > $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case GREATER_THAN_EQUAL:
				cypher.append("n.").append(property).append(" >= $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case LESS_THAN:
				cypher.append("n.").append(property).append(" < $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case LESS_THAN_EQUAL:
				cypher.append("n.").append(property).append(" <= $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case LIKE:
				cypher.append("n.").append(property).append(" =~ $").append(paramName);
				// Convert SQL LIKE to regex pattern
				String likeValue = String.valueOf(parameters[parameterIndex.get() - 1]);
				String regexPattern = "(?i)" + likeValue.replace("%", ".*").replace("_", ".");
				queryParameters.put(paramName, regexPattern);
				break;
			case STARTING_WITH:
				cypher.append("n.").append(property).append(" =~ $").append(paramName);
				queryParameters.put(paramName, "(?i)" + parameters[parameterIndex.get() - 1] + ".*");
				break;
			case ENDING_WITH:
				cypher.append("n.").append(property).append(" =~ $").append(paramName);
				queryParameters.put(paramName, "(?i).*" + parameters[parameterIndex.get() - 1]);
				break;
			case CONTAINING:
				cypher.append("n.").append(property).append(" =~ $").append(paramName);
				queryParameters.put(paramName, "(?i).*" + parameters[parameterIndex.get() - 1] + ".*");
				break;
			case NOT_CONTAINING:
				cypher.append("NOT n.").append(property).append(" =~ $").append(paramName);
				queryParameters.put(paramName, "(?i).*" + parameters[parameterIndex.get() - 1] + ".*");
				break;
			case IS_NULL:
				cypher.append("n.").append(property).append(" IS NULL");
				parameterIndex.decrementAndGet(); // No parameter consumed
				break;
			case IS_NOT_NULL:
				cypher.append("n.").append(property).append(" IS NOT NULL");
				parameterIndex.decrementAndGet(); // No parameter consumed
				break;
			case TRUE:
				cypher.append("n.").append(property).append(" = true");
				parameterIndex.decrementAndGet(); // No parameter consumed
				break;
			case FALSE:
				cypher.append("n.").append(property).append(" = false");
				parameterIndex.decrementAndGet(); // No parameter consumed
				break;
			case IN:
				cypher.append("n.").append(property).append(" IN $").append(paramName);
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			case NOT_IN:
				cypher.append("NOT (n.").append(property).append(" IN $").append(paramName).append(")");
				queryParameters.put(paramName, parameters[parameterIndex.get() - 1]);
				break;
			default:
				throw new IllegalArgumentException(
						"Unsupported query keyword: " + part.getType() + " for property: " + property);
		}
	}

}
