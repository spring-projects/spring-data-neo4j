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
