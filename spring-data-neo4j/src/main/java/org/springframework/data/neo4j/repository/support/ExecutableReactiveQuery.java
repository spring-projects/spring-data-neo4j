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

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.apiguardian.api.API;
import org.neo4j.driver.exceptions.NoSuchRecordException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;

/**
 * Interface for controlling query execution.
 *
 * @param <T> The type of the objects returned by this query.
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecutableReactiveQuery<T> {

	public static <T> ExecutableReactiveQuery<T> create(ReactiveNeo4jClient neo4jClient, PreparedQuery<T> preparedQuery) {

		Class<T> resultType = preparedQuery.getResultType();
		Neo4jClient.MappingSpec<Mono<T>, Flux<T>, T> mappingSpec = neo4jClient
			.newQuery(preparedQuery.getCypherQuery())
			.bindAll(preparedQuery.getParameters())
			.fetchAs(resultType);

		Neo4jClient.RecordFetchSpec<Mono<T>, Flux<T>, T> fetchSpec = preparedQuery
			.getOptionalMappingFunction()
			.map(mappingFunction -> mappingSpec.mappedBy(mappingFunction))
			.orElse(mappingSpec);

		return new ExecutableReactiveQuery<>(fetchSpec);
	}

	private final Neo4jClient.RecordFetchSpec<Mono<T>, Flux<T>, T> fetchSpec;

	/**
	 * @return All results returned by this query.
	 */
	public Flux<T> getResults() {
		return fetchSpec.all();
	}

	/**
	 * @return A single result
	 * @throws IncorrectResultSizeDataAccessException
	 */
	public Mono<T> getSingleResult() {
		try {
			return fetchSpec.one();
		} catch (NoSuchRecordException e) {
			// This exception is thrown by the driver in both cases when there are 0 or 1+n records
			// So there has been an incorrect result size, but not to few results but to many.
			throw new IncorrectResultSizeDataAccessException(1);
		}
	}

}
