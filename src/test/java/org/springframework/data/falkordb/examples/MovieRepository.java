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
package org.springframework.data.falkordb.examples;

import java.util.List;
import java.util.Optional;

import org.springframework.data.falkordb.repository.FalkorDBRepository;
import org.springframework.data.falkordb.repository.query.Query;
import org.springframework.data.repository.query.Param;

/**
 * Example repository interface demonstrating the {@link Query} annotation usage.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface MovieRepository extends FalkorDBRepository<Movie, String> {

	// Simple custom query with parameter binding by name
	@Query("MATCH (m:Movie) WHERE m.released > $year RETURN m")
	List<Movie> findMoviesReleasedAfter(@Param("year") Integer year);

	// Custom query with parameter binding by index
	@Query("MATCH (m:Movie) WHERE m.title CONTAINS $0 RETURN m")
	List<Movie> findMoviesByTitleContaining(String titlePart);

	// Complex query returning movie with actors
	@Query("MATCH (m:Movie {title: $title})-[r:ACTED_IN]-(p:Person) RETURN m, collect(r), collect(p)")
	Optional<Movie> findMovieWithActors(@Param("title") String title);

	// Query with entity parameter using special __id__ syntax
	@Query("MATCH (m:Movie {title: $movie.__id__})-[:ACTED_IN]-(p:Person) RETURN p")
	List<Person> findActorsInMovie(@Param("movie") Movie movie);

	// Count query
	@Query(value = "MATCH (m:Movie) WHERE m.released > $year RETURN count(m)", count = true)
	Long countMoviesReleasedAfter(@Param("year") Integer year);

	// Exists query
	@Query(value = "MATCH (m:Movie {title: $title}) RETURN count(m) > 0", exists = true)
	Boolean existsByTitle(@Param("title") String title);

	// Write operation query
	@Query(value = "MATCH (m:Movie {title: $title}) SET m.updated = timestamp() RETURN m", write = true)
	Movie updateMovieTimestamp(@Param("title") String title);

	// Query to find movies by actor name with relationship properties
	@Query("MATCH (m:Movie)-[r:ACTED_IN]-(p:Person {name: $actorName}) " +
		   "RETURN m, collect(r), collect(p)")
	List<Movie> findMoviesByActorName(@Param("actorName") String actorName);

	// Query to find co-actors (actors who acted in the same movie)
	@Query("MATCH (p1:Person {name: $actorName})-[:ACTED_IN]->(m:Movie)<-[:ACTED_IN]-(p2:Person) " +
		   "WHERE p1 <> p2 " +
		   "RETURN DISTINCT p2")
	List<Person> findCoActors(@Param("actorName") String actorName);

	// Query with multiple parameters
	@Query("MATCH (m:Movie) WHERE m.released >= $startYear AND m.released <= $endYear RETURN m")
	List<Movie> findMoviesInYearRange(@Param("startYear") Integer startYear, 
									  @Param("endYear") Integer endYear);

}