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

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.annotation.Depth;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

/**
 * @author Nicolas Mervaillie
 */
@Repository
public interface CinemaStreamingRepository extends Neo4jRepository<Cinema, Long> {

	Optional<Cinema> findByName(String name, @Depth int depth);

	@Query(value = "MATCH (n:Theatre) RETURN n;")
	Stream<Cinema> getAllCinemas();

	@Query("MATCH (n:Theatre) RETURN n")
	Stream<Cinema> getCinemasSortedByName(Sort sort);

	@Async
	@Query(value = "MATCH (n:Theatre) RETURN n;")
	CompletableFuture<List<Cinema>> getAllCinemasAsync();
}
