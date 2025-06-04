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
package org.springframework.data.neo4j.integration.issues.gh2533;

import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author Michael J. Simons
 */
public interface ReactiveGH2533Repository extends ReactiveNeo4jRepository<EntitiesAndProjections.GH2533Entity, Long> {

	@Query("MATCH p=(n)-[*0..1]->(m) WHERE id(n)=$id RETURN n, collect(relationships(p)), collect(m);")
	Mono<EntitiesAndProjections.GH2533Entity> findByIdWithLevelOneLinks(@Param("id") Long id);

}
