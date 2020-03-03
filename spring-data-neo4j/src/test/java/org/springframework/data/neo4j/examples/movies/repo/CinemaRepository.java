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
package org.springframework.data.neo4j.examples.movies.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.CinemaAndBlockbuster;
import org.springframework.data.neo4j.examples.movies.domain.CinemaAndBlockbusterName;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResult;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResultInterface;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
@Repository
public interface CinemaRepository extends Neo4jRepository<Cinema, Long> {

	Collection<Cinema> findByName(String name);

	List<Cinema> findByLocation(String location);

	List<Cinema> findByLocation(String location, @Depth int depth);

	List<Cinema> findByLocationLike(String location);

	List<Cinema> findByNameAndLocation(String name, String location);

	List<Cinema> findByNameOrLocation(String name, String location);

	List<Cinema> findByCapacityGreaterThan(int capacity);

	List<Cinema> findByCapacityLessThan(int capacity);

	List<Cinema> findByVisitedName(String name);

	List<Cinema> findByVisitedInterestedName(String title);

	List<Cinema> findByLocationAndVisitedName(String location, String name);

	List<Cinema> findByLocationAndCapacityGreaterThan(String location, int capacity);

	List<Cinema> findByCapacityLessThanAndLocation(int capacity, String location);

	List<Cinema> findByCapacityGreaterThanOrLocation(int capacity, String location);

	List<Cinema> findByLocationOrCapacityLessThan(String location, int capacity);

	List<Cinema> findByLocationOrVisitedName(String location, String name);

	// invalid query : not enough arguments
	List<Cinema> findByLocationOrVisitedName(String location);

	List<Cinema> findByVisitedNameAndBlockbusterOfTheWeekName(String location, String name);

	List<Cinema> findByVisitedNameOrBlockbusterOfTheWeekName(String location, String name);

	List<Cinema> findByVisitedNameAndVisitedMiddleName(String name, String middleName);

	List<Cinema> findByNameMatches(String name);

	Cinema findByName(String name, @Depth int depth);

	@Query(value = "MATCH (n:Theatre) RETURN n;", countQuery = "MATCH (n:Theatre) return count(*);")
	Page<Cinema> getPagedCinemas(Pageable pageable);

	@Query(value = "MATCH (n:Theatre) RETURN n;")
	Page<Cinema> getPagedCinemasWithoutCountQuery(Pageable pageable);

	@Query(value = "MATCH (n:Theatre) RETURN n", countQuery = "MATCH (n:Theatre) return count(*)")
	Page<CinemaQueryResult> getPagedCinemaQueryResults(Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	Slice<CinemaQueryResult> getSlicedCinemaQueryResults(Pageable pageable);

	@Query(value = "MATCH (n:Theatre) RETURN n", countQuery = "MATCH (n:Theatre) return count(*)")
	Page<CinemaQueryResultInterface> getPagedCinemaQueryResultInterfaces(Pageable pageable);

	@Query(value = "MATCH (n:Theatre {city:$city}) RETURN n ORDER BY n.name",
			countQuery = "MATCH (n:Theatre {city:$city}) return count(*)")
	Page<Cinema> getPagedCinemasByCityWithPageCount(@Param("city") String city, Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	Slice<Cinema> getSlicedCinemasByName(Pageable pageable);

	Page<Cinema> findByLocation(String city, Pageable pageable);

	Page<Cinema> findByLocationAndVisitedName(String location, String name, Pageable pageable);

	Page<Cinema> findByVisitedName(String name, Pageable pageable);

	List<Cinema> findByLocation(String city, Sort sort);

	Page<Cinema> findByCapacity(int capacity, Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	List<Cinema> getCinemasSortedByName(Sort sort);

	List<CinemaAndBlockbuster> findByNameLike(String name);

	List<CinemaAndBlockbuster> findByNameLike(String name, Sort sort);

	List<CinemaAndBlockbusterName> findByNameStartingWith(String nameStart);

	List<CinemaAndBlockbuster> findByNameLike(String name, @Depth int depth);
}
