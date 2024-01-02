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
package org.springframework.data.neo4j.integration.issues.gh2572;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
public interface ReactiveGH2572Repository extends ReactiveNeo4jRepository<GH2572Child, String> {

	@Query("MATCH(person:GH2572Parent {id: $id}) "
			+ "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			+ "RETURN dog")
	Flux<GH2572Child> getDogsForPerson(String id);

	@Query("MATCH(person:GH2572Parent {id: $id}) "
			+ "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			+ "RETURN dog ORDER BY dog.name ASC LIMIT 1")
	Mono<GH2572Child> findOneDogForPerson(String id);
}
