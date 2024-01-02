/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.repository;

import org.apiguardian.api.API;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * Throw when a query doesn't return a required result.
 *
 * @author Michael J. Simons
 * @soundtrack Deichkind - Niveau weshalb warum
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public class NoResultException extends EmptyResultDataAccessException {

	private final String query;

	public NoResultException(int expectedNumberOfResults, String query) {
		super(expectedNumberOfResults);
		this.query = query;
	}

	public String getQuery() {
		return query;
	}
}
