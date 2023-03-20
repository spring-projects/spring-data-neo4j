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
package org.springframework.data.neo4j.integration.imperative.repositories;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.driver.types.Point;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Polygon;
import org.springframework.data.neo4j.integration.shared.common.DtoPersonProjection;
import org.springframework.data.neo4j.integration.shared.common.DtoPersonProjectionContainingAdditionalFields;
import org.springframework.data.neo4j.integration.shared.common.PersonProjection;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.BoundingBox;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.repository.query.Param;
import org.springframework.data.util.Streamable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Gerrit Meier
 * @author Michael J. Simon
 */
public interface PersonRepository extends Neo4jRepository<PersonWithAllConstructor, Long> {

	@Transactional
	@Query("RETURN 1")
	Long customQuery();

	@Query("MATCH (n:PersonWithAllConstructor) return collect(n)")
	List<PersonWithAllConstructor> aggregateAllPeople();

	/**
	 * A custom aggregate that allows for something like getFriend1, 2 or other stuff...
	 */
	class CustomAggregation implements Streamable<PersonWithAllConstructor> {

		private final Streamable<PersonWithAllConstructor> delegate;

		public CustomAggregation(Streamable<PersonWithAllConstructor> delegate) {
			this.delegate = delegate;
		}

		@Override public Iterator<PersonWithAllConstructor> iterator() {
			return delegate.iterator();
		}
	}

	@Query("MATCH (n:PersonWithAllConstructor) return collect(n)")
	CustomAggregation aggregateAllPeopleCustom();

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	List<PersonWithAllConstructor> getAllPersonsViaQuery();

	@Query("MATCH (n) WHERE 1 = 2 return n")
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

	@Query("MATCH (n:PersonWithAllConstructor{name::#{#part1 + #part2}}) return n :#{orderBy(#sort)}")
	Optional<PersonWithAllConstructor> getOptionalPersonViaQueryWithSort(@Param("part1") String part1,
			@Param("part2") String part2, Sort sort);

	Optional<PersonWithAllConstructor> getOptionalPersonViaNamedQuery(@Param("part1") String part1,
			@Param("part2") String part2);

	// Derived finders, should be extracted into another repo.
	Optional<PersonWithAllConstructor> findOneByNameAndFirstName(String name, String firstName);

	Page<PersonWithAllConstructor> findAllByNameOrName(Pageable pageable, String aName, String anotherName);

	Page<PersonWithAllConstructor> findAllByNameOrName(String aName, String anotherName, Pageable pageable);

	Slice<PersonWithAllConstructor> findSliceByNameOrName(String aName, String anotherName, Pageable pageable);

	Window<PersonWithAllConstructor> findTop1ByOrderByName(ScrollPosition scrollPosition);

	@Query("MATCH (n:PersonWithAllConstructor) WHERE n.name = $aName OR n.name = $anotherName RETURN n ORDER BY n.name DESC SKIP $skip LIMIT $limit")
	Slice<PersonWithAllConstructor> findSliceByCustomQueryWithoutCount(@Param("aName") String aName, @Param("anotherName") String anotherName, Pageable pageable);

	@Query(value = "MATCH (n:PersonWithAllConstructor) WHERE n.name = $aName OR n.name = $anotherName RETURN n ORDER BY n.name DESC SKIP $skip LIMIT $limit",
	countQuery = "MATCH (n:PersonWithAllConstructor) WHERE n.name = $aName OR n.name = $anotherName RETURN count(n)")
	Slice<PersonWithAllConstructor> findSliceByCustomQueryWithCount(@Param("aName") String aName, @Param("anotherName") String anotherName, Pageable pageable);

	@Query(value = "MATCH (n:PersonWithAllConstructor) WHERE n.name = $aName OR n.name = $anotherName RETURN n :#{orderBy(#pageable)} SKIP $skip LIMIT $limit",
			countQuery = "MATCH (n:PersonWithAllConstructor) WHERE n.name = $aName OR n.name = $anotherName RETURN count(n)")
	Page<PersonWithAllConstructor> findPageByCustomQueryWithCount(@Param("aName") String aName, @Param("anotherName") String anotherName, Pageable pageable);

	Long countAllByNameOrName(String aName, String anotherName);

	Optional<PersonWithAllConstructor> findOneByNameAndFirstNameAllIgnoreCase(String name, String firstName);

	PersonWithAllConstructor findOneByName(String name);

	List<PersonWithAllConstructor> findAllByNameOrName(String aName, String anotherName);

	Stream<PersonWithAllConstructor> findAllByNameLike(String aName);

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

	boolean existsByName(String name);

	List<PersonWithAllConstructor> findAllByPlace(GeographicPoint2d p);

	List<PersonWithAllConstructor> findAllByPlace(SomethingThatIsNotKnownAsEntity p);

	/**
	 * Needed to have something that is not mapped in to a map.
	 */
	class SomethingThatIsNotKnownAsEntity {
	}

	List<PersonWithAllConstructor> findAllByPlaceNear(Point p);

	List<PersonWithAllConstructor> findAllByPlaceNear(Point p, Distance max);

	List<PersonWithAllConstructor> findAllByPlaceNear(Point p, Range<Distance> between);

	List<PersonWithAllConstructor> findAllByPlaceNear(Range<Distance> between, Point p);

	List<PersonWithAllConstructor> findAllByPlaceNearAndFirstNameIn(Point p, List<String> haystack);

	List<PersonWithAllConstructor> findAllByPlaceNearAndFirstNameAllIgnoreCase(Point p, String firstName);

	List<PersonWithAllConstructor> findAllByPlaceWithin(Circle circle);

	List<PersonWithAllConstructor> findAllByPlaceWithin(Box box);

	List<PersonWithAllConstructor> findAllByPlaceWithin(BoundingBox box);

	List<PersonWithAllConstructor> findAllByPlaceWithin(Polygon polygon);

	List<PersonWithAllConstructor> findAllByOrderByFirstNameAscBornOnDesc();

	// TODO Integration tests for failed validations
	// List<PersonWithAllConstructor> findAllByBornOnAfter(String date);
	// List<PersonWithAllConstructor> findAllByNameOrPersonNumberIsBetweenAndFirstNameNotInAndFirstNameEquals(String name,
	// Long low, Long high, String wrong, List<String> haystack);
	// List<PersonWithAllConstructor>
	// findAllByNameOrPersonNumberIsBetweenAndCoolIsTrueAndFirstNameNotInAndFirstNameEquals(String name, Long low, Long
	// high, String wrong, List<String> haystack);
	// List<PersonWithAllConstructor> findAllByNameNotEmpty();
	// List<PersonWithAllConstructor> findAllByPlaceNear(Point p);
	// List<PersonWithAllConstructor> findAllByPlaceNear(Point p, String);

	PersonProjection findByName(String name);

	List<PersonProjection> findBySameValue(String sameValue);

	List<DtoPersonProjection> findByFirstName(String firstName);

	Optional<DtoPersonProjection> findOneByFirstName(String firstName);

	DtoPersonProjection findOneByNullable(String nullable);

	@Query(""
			+ "MATCH (n:PersonWithAllConstructor) where n.name = $name "
			+ "WITH n MATCH(m:PersonWithAllConstructor) WHERE id(n) <> id(m) "
			+ "RETURN n, collect(m) AS otherPeople, 4711 AS someLongValue, [21.42, 42.21] AS someDoubles")
	List<DtoPersonProjectionContainingAdditionalFields> findAllDtoProjectionsWithAdditionalProperties(@Param("name") String name);

	/**
	 * A custom aggregate that allows for something like getFriend1, 2 or other stuff...
	 */
	class CustomAggregationOfDto implements Streamable<DtoPersonProjectionContainingAdditionalFields> {

		private final Streamable<DtoPersonProjectionContainingAdditionalFields> delegate;

		public CustomAggregationOfDto(Streamable<DtoPersonProjectionContainingAdditionalFields> delegate) {
			this.delegate = delegate;
		}

		@Override public Iterator<DtoPersonProjectionContainingAdditionalFields> iterator() {
			return delegate.iterator();
		}

		public DtoPersonProjectionContainingAdditionalFields getBySomeLongValue(long value) {

			return delegate.stream()
					.collect(Collectors.toMap(DtoPersonProjectionContainingAdditionalFields::getSomeLongValue, Function.identity()))
					.get(value);
		}
	}

	@Query(""
		   + "MATCH (n:PersonWithAllConstructor) where n.name = $name "
		   + "WITH n MATCH(m:PersonWithAllConstructor) WHERE id(n) <> id(m)" +
			" WITH n, collect(m) as ms "
		   + "RETURN [{n: n, otherPeople: ms, someLongValue: 4711, someDoubles: [21.42, 42.21]}]")
	CustomAggregationOfDto findAllDtoProjectionsWithAdditionalPropertiesAsCustomAggregation(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) where n.name = $name return n{.name}")
	PersonProjection findByNameWithCustomQueryAndMapProjection(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) return n{.name}")
	List<PersonProjection> loadAllProjectionsWithMapProjection();

	@Query("MATCH (n:PersonWithAllConstructor) where n.name = $name return n")
	PersonProjection findByNameWithCustomQueryAndNodeReturn(@Param("name") String name);

	@Query("MATCH (n:PersonWithAllConstructor) return n")
	List<PersonProjection> loadAllProjectionsWithNodeReturn();

	@Query("CREATE (n:PersonWithAllConstructor) SET n+= $testNode.__properties__ RETURN n")
	PersonWithAllConstructor createWithCustomQuery(@Param("testNode") PersonWithAllConstructor testNode);

	@Query(value = "MATCH (n:PersonWithAllConstructor) RETURN n :#{ orderBy (#pageable.sort)} SKIP $skip LIMIT $limit")
	List<PersonWithAllConstructor> orderBySpel(Pageable page);

	void deleteAllByName(String name);

	long deleteAllByNameOrName(String name, String otherName);
}
