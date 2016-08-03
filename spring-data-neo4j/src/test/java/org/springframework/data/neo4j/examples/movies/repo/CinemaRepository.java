/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.examples.movies.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResultInterface;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.CinemaQueryResult;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
@Repository
public interface CinemaRepository extends GraphRepository<Cinema> {

	Collection<Cinema> findByName(String name);

	List<Cinema> findByLocation(String location);

	List<Cinema> findByLocationLike(String location);

	List<Cinema> findByNameAndLocation(String name, String location);

	List<Cinema> findByNameOrLocation(String name, String location);

	List<Cinema> findByCapacityGreaterThan(int capacity);

	List<Cinema> findByCapacityLessThan(int capacity);

	List<Cinema> findByVisitedName(String name);

	List<Cinema> findByLocationAndVisitedName(String location, String name);

	List<Cinema> findByLocationAndCapacityGreaterThan(String location, int capacity);

	List<Cinema> findByCapacityLessThanAndLocation(int capacity, String location);

	List<Cinema> findByCapacityGreaterThanOrLocation(int capacity, String location);

	List<Cinema> findByLocationOrCapacityLessThan(String location, int capacity);

	List<Cinema> findByLocationOrVisitedName(String location, String name);

	List<Cinema> findByVisitedNameAndBlockbusterOfTheWeekName(String location, String name);

	List<Cinema> findByVisitedNameOrBlockbusterOfTheWeekName(String location, String name);

	List<Cinema> findByVisitedNameAndVisitedMiddleName(String name, String middleName);

	List<Cinema> findByNameMatches(String name);

	Cinema findByName(String name, @Depth int depth);

	@Query("MATCH (n:Theatre) RETURN n")
	Page<Cinema> getPagedCinemas(Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	Page<CinemaQueryResult> getPagedCinemaQueryResults(Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	Page<CinemaQueryResultInterface> getPagedCinemaQueryResultInterfaces(Pageable pageable);

	@Query(value = "MATCH (n:Theatre) RETURN n ORDER BY n.name", countQuery = "MATCH (n:Theatre) return count(*)")
	Page<Cinema> getPagedCinemasWithPageCount(Pageable pageable);

	@Query(value = "MATCH (n:Theatre {city:{city}}) RETURN n ORDER BY n.name", countQuery = "MATCH (n:Theatre {city:{city}}) return count(*)")
	Page<Cinema> getPagedCinemasByCityWithPageCount(@Param("city") String city, Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	Slice<Cinema> getSlicedCinemasByName(Pageable pageable);

	Page<Cinema> findByLocation(String city, Pageable pageable);

	Page<Cinema> findByLocationAndVisitedName(String location, String name, Pageable pageable);

	Page<Cinema> findByVisitedName(String name, Pageable pageable);

	List<Cinema> findByLocation(String city, Sort sort);

	List<Cinema> findByCapacity(int capacity, Pageable pageable);

	@Query("MATCH (n:Theatre) RETURN n")
	List<Cinema> getCinemasSortedByName(Sort sort);
}
