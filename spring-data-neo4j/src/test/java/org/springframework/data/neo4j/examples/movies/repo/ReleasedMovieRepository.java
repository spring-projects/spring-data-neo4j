/*
 * Copyright (c) 2018 "Neo4j, Inc." / "Pivotal Software, Inc."
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

import java.util.Date;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.ReleasedMovie;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Michael J. Simonsn
 */
public interface ReleasedMovieRepository extends Neo4jRepository<ReleasedMovie, Long> {
	@Query("MATCH (n:ReleasedMovie) RETURN MAX(n.cinemaRelease)")
	Date findMaxCinemaReleaseDate();

	@Query("MATCH (n:ReleasedMovie) RETURN MAX(n.cannesRelease)")
	Date findMaxCannesReleaseDate();
}
