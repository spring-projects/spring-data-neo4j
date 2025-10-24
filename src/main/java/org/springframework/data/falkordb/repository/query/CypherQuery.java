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
import java.util.Map;

/**
 * Represents a Cypher query with parameters.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
class CypherQuery {

	/**
	 * The Cypher query string.
	 */
	private final String query;

	/**
	 * The query parameters.
	 */
	private final Map<String, Object> parameters;

	/**
	 * Creates a new CypherQuery.
	 * @param queryString the Cypher query string
	 * @param queryParams the query parameters
	 */
	CypherQuery(final String queryString, final Map<String, Object> queryParams) {
		this.query = queryString;
		this.parameters = (queryParams != null) ? queryParams : new HashMap<>();
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
