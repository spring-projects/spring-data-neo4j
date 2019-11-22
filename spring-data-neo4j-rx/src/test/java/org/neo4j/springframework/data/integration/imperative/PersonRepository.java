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
package org.neo4j.springframework.data.integration.imperative;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.neo4j.driver.types.Point;
import org.neo4j.springframework.data.integration.shared.DtoPersonProjection;
import org.neo4j.springframework.data.integration.shared.KotlinPerson;
import org.neo4j.springframework.data.integration.shared.PersonProjection;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.integration.shared.PersonWithNoConstructor;
import org.neo4j.springframework.data.integration.shared.PersonWithWither;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.query.Query;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.repository.query.Param;
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

	@Query("MATCH (n:UnknownLabel) return n")
	List<PersonWithAllConstructor> getNobodyViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor{name:'Test'}) return n")
	PersonWithAllConstructor getOnePersonViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor{name:'Test'}) return n")
	Optional<PersonWithAllConstructor> getOptionalPersonViaQuery();

	@Query("MATCH (n:PersonWithAllConstructor{name:$name}) return n")
	Optional<PersonWithAllConstructor> getOptionalPersonViaQuery(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor{name::#{#part1 + #part2}}) return n")
	Optional<PersonWithAllConstructor> getOptionalPersonViaQuery(@Param("part1") String part1,
		@Param("part2") String part2);

	Optional<PersonWithAllConstructor> getOptionalPersonViaNamedQuery(@Param("part1") String part1,
		@Param("part2") String part2);

	@Query("MATCH (n:PersonWithNoConstructor) return n")
	List<PersonWithNoConstructor> getAllPersonsWithNoConstructorViaQuery();

	@Query("MATCH (n:PersonWithNoConstructor{name:'Test'}) return n")
	PersonWithNoConstructor getOnePersonWithNoConstructorViaQuery();

	@Query("MATCH (n:PersonWithNoConstructor{name:'Test'}) return n")
	Optional<PersonWithNoConstructor> getOptionalPersonWithNoConstructorViaQuery();

	@Query("MATCH (n:PersonWithWither) return n")
	List<PersonWithWither> getAllPersonsWithWitherViaQuery();

	@Query("MATCH (n:PersonWithWither{name:'Test'}) return n")
	PersonWithWither getOnePersonWithWitherViaQuery();

	@Query("MATCH (n:PersonWithWither{name:'Test'}) return n")
	Optional<PersonWithWither> getOptionalPersonWithWitherViaQuery();

	@Query("MATCH (n:KotlinPerson) return n")
	List<KotlinPerson> getAllKotlinPersonsViaQuery();

	@Query("MATCH (n:KotlinPerson{name:'Test'}) return n")
	KotlinPerson getOneKotlinPersonViaQuery();

	@Query("MATCH (n:KotlinPerson{name:'Test'}) return n")
	Optional<KotlinPerson> getOptionalKotlinPersonViaQuery();

	// Derived finders, should be extracted into another repo.
	Optional<PersonWithAllConstructor> findOneByNameAndFirstName(String name, String firstName);

	Optional<PersonWithAllConstructor> findOneByNameAndFirstNameAllIgnoreCase(String name, String firstName);

	PersonWithAllConstructor findOneByName(String name);

	List<PersonWithAllConstructor> findAllByNameOrName(String aName, String anotherName);

	Stream<PersonWithAllConstructor> findAllByNameLike(String aName);

	CompletableFuture<PersonWithAllConstructor> findOneByFirstName(String aName);

	List<PersonWithAllConstructor> findAllBySameValue(String sameValue);

	List<PersonWithAllConstructor> findAllBySameValueIgnoreCase(String sameValue);

	List<PersonWithAllConstructor> findAllByNameNot(String name);

	List<PersonWithAllConstructor> findAllByNameNotIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByFirstNameLike(String name);

	List<PersonWithAllConstructor> findAllByFirstNameLikeIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByFirstNameMatches(String name);

	List<PersonWithAllConstructor> findAllByFirstNameNotLike(String name);

	List<PersonWithAllConstructor> findAllByFirstNameNotLikeIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByCoolTrue();

	List<PersonWithAllConstructor> findAllByCoolFalse();

	List<PersonWithAllConstructor> findAllByFirstNameStartingWith(String name);

	List<PersonWithAllConstructor> findAllByFirstNameStartingWithIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByFirstNameContaining(String name);

	List<PersonWithAllConstructor> findAllByFirstNameContainingIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByFirstNameNotContaining(String name);

	List<PersonWithAllConstructor> findAllByFirstNameNotContainingIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByFirstNameEndingWith(String name);

	List<PersonWithAllConstructor> findAllByFirstNameEndingWithIgnoreCase(String name);

	List<PersonWithAllConstructor> findAllByPersonNumberIsLessThan(Long number);

	List<PersonWithAllConstructor> findAllByPersonNumberIsLessThanEqual(Long number);

	List<PersonWithAllConstructor> findAllByPersonNumberIsGreaterThanEqual(Long number);

	List<PersonWithAllConstructor> findAllByPersonNumberIsGreaterThan(Long number);

	List<PersonWithAllConstructor> findAllByPersonNumberIsBetween(Range range);

	List<PersonWithAllConstructor> findAllByPersonNumberIsBetween(Long low, Long high);

	List<PersonWithAllConstructor> findAllByBornOn(LocalDate date);

	List<PersonWithAllConstructor> findAllByBornOnAfter(LocalDate date);

	List<PersonWithAllConstructor> findAllByBornOnBefore(LocalDate date);

	List<PersonWithAllConstructor> findAllByCreatedAtBefore(Instant instant);

	List<PersonWithAllConstructor> findAllByNullableIsNotNull();

	List<PersonWithAllConstructor> findAllByNullableIsNull();

	List<PersonWithAllConstructor> findAllByFirstNameIn(List<String> haystack);

	List<PersonWithAllConstructor> findAllByFirstNameNotIn(List<String> haystack);

	List<PersonWithAllConstructor> findAllByThingsIsEmpty();

	List<PersonWithAllConstructor> findAllByThingsIsNotEmpty();

	List<PersonWithAllConstructor> findAllByNullableExists();

	List<PersonWithAllConstructor> findAllByPlaceNear(Point p);

	List<PersonWithAllConstructor> findAllByPlaceNear(Point p, Distance max);

	List<PersonWithAllConstructor> findAllByPlaceNear(Point p, Range<Distance> between);

	List<PersonWithAllConstructor> findAllByPlaceNear(Range<Distance> between, Point p);

	List<PersonWithAllConstructor> findAllByPlaceNearAndFirstNameIn(Point p, List<String> haystack);

	List<PersonWithAllConstructor> findAllByPlaceNearAndFirstNameAllIgnoreCase(Point p, String firstName);

	List<PersonWithAllConstructor> findAllByPlaceWithin(Circle circle);

	List<PersonWithAllConstructor> findAllByOrderByFirstNameAscBornOnDesc();

	// TODO Integration tests for failed validations
	// 	List<PersonWithAllConstructor> findAllByBornOnAfter(String date);
	// List<PersonWithAllConstructor> findAllByNameOrPersonNumberIsBetweenAndFirstNameNotInAndFirstNameEquals(String name, Long low, Long high, String wrong, List<String> haystack);
	// List<PersonWithAllConstructor> findAllByNameOrPersonNumberIsBetweenAndCoolIsTrueAndFirstNameNotInAndFirstNameEquals(String name, Long low, Long high, String wrong, List<String> haystack);
	// List<PersonWithAllConstructor> findAllByNameNotEmpty();
	// List<PersonWithAllConstructor> findAllByPlaceNear(Point p);
	// List<PersonWithAllConstructor> findAllByPlaceNear(Point p, String);

	PersonProjection findByName(String name);

	List<PersonProjection> findBySameValue(String sameValue);

	List<DtoPersonProjection> findByFirstName(String firstName);

	@Query("MATCH (n:PersonWithAllConstructor) where n.name = $name return n{.name}")
	PersonProjection findByNameWithCustomQueryAndMapProjection(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) return n{.name}")
	List<PersonProjection> loadAllProjectionsWithMapProjection();

	@Query("MATCH (n:PersonWithAllConstructor) where n.name = $name return n")
	PersonProjection findByNameWithCustomQueryAndNodeReturn(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	List<PersonProjection> loadAllProjectionsWithNodeReturn();
}
