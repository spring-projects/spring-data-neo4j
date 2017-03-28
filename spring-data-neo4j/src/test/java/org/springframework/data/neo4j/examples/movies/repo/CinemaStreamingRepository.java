/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
