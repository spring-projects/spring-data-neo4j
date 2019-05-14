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
package org.springframework.data.neo4j.integration;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gerrit Meier
 * @author Michael J. Simon
 */
public interface PersonRepository extends Neo4jRepository<PersonWithAllConstructor, Long> {

	@Transactional
	@Query("RETURN 1")
	Long customQuery();

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	List<PersonWithAllConstructor> getAllPersonsViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor{name:'Test'}) return n")
	PersonWithAllConstructor getOnePersonViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor{name:'Test'}) return n")
	Optional<PersonWithAllConstructor> getOptionalPersonsViaQuery();

	@Query("MATCH (n:PersonWithNoConstructor) return n")
	List<PersonWithNoConstructor> getAllPersonsWithNoConstructorViaQuery();

	@Query("MATCH (n:PersonWithNoConstructor{name:'Test'}) return n")
	PersonWithNoConstructor getOnePersonWithNoConstructorViaQuery();

	@Query("MATCH (n:PersonWithNoConstructor{name:'Test'}) return n")
	Optional<PersonWithNoConstructor> getOptionalPersonsWithNoConstructorViaQuery();

	@Query("MATCH (n:PersonWithWither) return n")
	List<PersonWithWither> getAllPersonsWithWitherViaQuery();

	@Query("MATCH (n:PersonWithWither{name:'Test'}) return n")
	PersonWithWither getOnePersonWithWitherViaQuery();

	@Query("MATCH (n:PersonWithWither{name:'Test'}) return n")
	Optional<PersonWithWither> getOptionalPersonsWithWitherViaQuery();

	// Derived finders, should be extracted into another repo.
	Optional<PersonWithAllConstructor> findOneByNameAndFirstName(String name, String firstName);

	List<PersonWithAllConstructor> findAllByNameOrName(String aName, String anotherName);

	List<PersonWithAllConstructor> findAllBySameValue(String sameValue);

	List<PersonWithAllConstructor> findAllByNameNot(String name);

	List<PersonWithAllConstructor> findAllByFirstNameLike(String name);

	List<PersonWithAllConstructor> findAllByFirstNameMatches(String name);

	List<PersonWithAllConstructor> findAllByFirstNameNotLike(String name);

	List<PersonWithAllConstructor> findAllByCoolTrue();

	List<PersonWithAllConstructor> findAllByCoolFalse();

	List<PersonWithAllConstructor> findAllByFirstNameStartingWith(String name);

	List<PersonWithAllConstructor> findAllByFirstNameContaining(String name);

	List<PersonWithAllConstructor> findAllByFirstNameNotContaining(String name);

	List<PersonWithAllConstructor> findAllByFirstNameEndingWith(String name);
}
