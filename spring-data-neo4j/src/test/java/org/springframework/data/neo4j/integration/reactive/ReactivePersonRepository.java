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
package org.springframework.data.neo4j.integration.reactive;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.integration.shared.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gerrit Meier
 */
public interface ReactivePersonRepository extends ReactiveNeo4jRepository<PersonWithAllConstructor, Long> {

	@Transactional
	@Query("RETURN 1")
	Mono<Long> customQuery();

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	Flux<PersonWithAllConstructor> getAllPersonsViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor{name:'Test'}) return n")
	Mono<PersonWithAllConstructor> getOnePersonViaQuery();

	Mono<PersonWithAllConstructor> findOneByNameAndFirstName(String name, String firstName);

	Mono<PersonWithAllConstructor> findOneByNameAndFirstNameAllIgnoreCase(String name, String firstName);

	Flux<PersonWithAllConstructor> findAllByNameOrName(String aName, String anotherName);

	Flux<PersonWithAllConstructor> findAllBySameValue(String sameValue);
}
