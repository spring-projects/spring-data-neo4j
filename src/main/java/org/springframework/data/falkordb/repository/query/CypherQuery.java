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
	CypherQuery(final String query, final Map<String, Object> parameters) {
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
