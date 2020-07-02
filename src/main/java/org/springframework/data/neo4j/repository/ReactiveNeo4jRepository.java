/*
 * Copyright 2011-2020 the original author or authors.
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

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;

/**
 * Neo4j specific {@link org.springframework.data.repository.Repository} interface with reactive support.
 *
 * @author Michael J. Simons
 * @param <T> type of the domain class to map
 * @param <ID> identifier type in the domain class
 * @since 1.0
 */
@NoRepositoryBean
public interface ReactiveNeo4jRepository<T, ID>
	extends ReactiveSortingRepository<T, ID>, ReactiveQueryByExampleExecutor<T> {
}
