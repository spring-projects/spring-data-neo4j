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

import org.springframework.data.domain.Sort;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBPersistentEntity;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
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

		// Simple implementation - for now, we'll support basic queries
		// FIXME: Parse PartTree to build WHERE conditions properly
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
