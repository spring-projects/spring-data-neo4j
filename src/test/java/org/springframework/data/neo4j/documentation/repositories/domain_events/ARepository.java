/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.documentation.repositories.domain_events;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * Repository for documentation purposes.
 */
// tag::standard-parameter[]
// tag::spel[]
public interface ARepository extends Neo4jRepository<AnAggregateRoot, String> {

	// end::standard-parameter[]
	// end::spel[]
	Optional<AnAggregateRoot> findByName(String name);

	// tag::standard-parameter[]
	@Query("MATCH (a:AnAggregateRoot {name: $name}) RETURN a") // <.>
	Optional<AnAggregateRoot> findByCustomQuery(String name);
	// end::standard-parameter[]

	// tag::spel[]
	@Query("MATCH (a:AnAggregateRoot) WHERE a.name = :#{#pt1 + #pt2} RETURN a")
	Optional<AnAggregateRoot> findByCustomQueryWithSpEL(String pt1, String pt2);
	// tag::standard-parameter[]
}
// end::standard-parameter[]
// end::spel[]
