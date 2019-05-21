/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.repository.NoResultException;

/**
 * Interface for controlling query execution.
 *
 * @param <T> The type of the objects returned by this query.
 * @author Michael J. Simons
 * @soundtrack Deichkind - Niveau weshalb warum
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface ExecutableQuery<T> {

	static <T> ExecutableQuery<T> create(PreparedQuery<T> description, Neo4jClient neo4jClient) {

		Class<T> resultType = description.getResultType();
		Neo4jClient.MappingSpec<Optional<T>, Collection<T>, T> mappingSpec = neo4jClient
			.newQuery(description.getCypherQuery())
			.bindAll(description.getParameters())
			.fetchAs(resultType);
		Neo4jClient.RecordFetchSpec<Optional<T>, Collection<T>, T> fetchSpec = description.getOptionalMappingFunction()
			.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction))
			.orElse(mappingSpec);

		return new DefaultExecutableQuery<>(description, fetchSpec);
	}

	/**
	 * @return All results returned by this query.
	 */
	List<T> getResults();

	/**
	 * @return A single result
	 * @throws IncorrectResultSizeDataAccessException
	 */
	Optional<T> getSingleResult();

	/**
	 * @return A single result
	 * @throws NoResultException
	 */
	T getRequiredSingleResult();
}
