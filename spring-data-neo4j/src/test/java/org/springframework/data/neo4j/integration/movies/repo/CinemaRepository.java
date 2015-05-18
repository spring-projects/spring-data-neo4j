/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.movies.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.neo4j.integration.movies.domain.Cinema;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
@Repository
public interface CinemaRepository extends GraphRepository<Cinema> {

	Collection<Cinema> findByName(String name);

	List<Cinema> findByLocation(String location);

	List<Cinema> findByNameAndLocation(String name, String location);

	List<Cinema> findByNameOrLocation(String name, String location);

	List<Cinema> findByCapacityGreaterThan(int capacity);

	List<Cinema> findByCapacityLessThan(int capacity);

	List<Cinema> findByVisitedName(String name);

	List<Cinema> findByLocationAndVisitedName(String location, String name);
}
