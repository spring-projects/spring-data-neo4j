/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.reactive.repositories;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.integration.shared.common.DtoPersonProjection;
import org.springframework.data.neo4j.integration.shared.common.PersonProjection;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public interface ReactivePersonRepository extends ReactiveNeo4jRepository<PersonWithAllConstructor, Long> {

	@Transactional
	@Query("RETURN 1")
	Mono<Long> customQuery();

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	Flux<PersonWithAllConstructor> getAllPersonsViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor) return collect(n)")
	Flux<PersonWithAllConstructor> aggregateAllPeople();

	@Query("MATCH (n:PersonWithAllConstructor{name:'Test'}) return n")
	Mono<PersonWithAllConstructor> getOnePersonViaQuery();

	Mono<PersonWithAllConstructor> findOneByNameAndFirstName(String name, String firstName);

	Mono<PersonWithAllConstructor> findOneByNameAndFirstNameAllIgnoreCase(String name, String firstName);

	Mono<Long> countAllByNameOrName(String aName, String anotherName);

	Flux<PersonWithAllConstructor> findAllByNameOrName(String aName, String anotherName);

	Flux<PersonWithAllConstructor> findAllBySameValue(String sameValue);

	Mono<PersonProjection> findByName(String name);

	Flux<PersonProjection> findBySameValue(String sameValue);

	Flux<DtoPersonProjection> findByFirstName(String firstName);

	Flux<PersonWithAllConstructor> findByNameStartingWith(String name, Pageable pageable);

	Flux<PersonWithAllConstructor> findAllByPlace(GeographicPoint2d p);

	Flux<PersonWithAllConstructor> findAllByPlace(SomethingThatIsNotKnownAsEntity p);

	/**
	 * Needed to have something that is not mapped in to a map.
	 */
	class SomethingThatIsNotKnownAsEntity {
	}

	@Query("MATCH (n:PersonWithAllConstructor) where n.name = $name return n{.name}")
	Mono<PersonProjection> findByNameWithCustomQueryAndMapProjection(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) return n{.name}")
	Flux<PersonProjection> loadAllProjectionsWithMapProjection();

	@Query("MATCH (n:PersonWithAllConstructor) where n.name = $name return n")
	Mono<PersonProjection> findByNameWithCustomQueryAndNodeReturn(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	Flux<PersonProjection> loadAllProjectionsWithNodeReturn();

	Mono<DtoPersonProjection> findOneByFirstName(String firstName);

	@Query("MATCH (n:PersonWithAllConstructor{name::#{#part1 + #part2}}) return n")
	Flux<PersonWithAllConstructor> getOptionalPersonViaQuery(@Param("part1") String part1,
			@Param("part2") String part2);

	@Query("MATCH (n:PersonWithAllConstructor{name::#{#part1 + #part2}}) return n :#{orderBy(#sort)}")
	Flux<PersonWithAllConstructor> getOptionalPersonViaQueryWithSort(@Param("part1") String part1,
			@Param("part2") String part2, Sort sort);

	Flux<PersonWithAllConstructor> getOptionalPersonViaNamedQuery(@Param("part1") String part1,
			@Param("part2") String part2);

	Mono<Void> deleteAllByName(String name);

	Mono<Long> deleteAllByNameOrName(String name, String otherName);
}
