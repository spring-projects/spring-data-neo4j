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

package org.springframework.data.neo4j.examples.restaurants.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Jasper Blues
 */
public interface RestaurantRepository extends Neo4jRepository<Restaurant, Long> {

	List<Restaurant> findByNameAndLocationNear(String name, Distance distance, Point point);

	List<Restaurant> findByLocationNearAndName(Distance distance, Point point, String name);

	List<Restaurant> findByScoreBetween(double min, double max);

	List<Restaurant> findByScoreLessThan(double max);

	List<Restaurant> findByScoreLessThanEqual(double max);

	List<Restaurant> findByScoreGreaterThan(double max);

	List<Restaurant> findByScoreGreaterThanEqual(double max);

	List<Restaurant> findByDescriptionIsNull();

	List<Restaurant> findByDescriptionIsNotNull();

	List<Restaurant> findByLaunchDateBefore(Date date);

	List<Restaurant> findByLaunchDateAfter(Date date);

	List<Restaurant> findByNameNotLike(String name);

	List<Restaurant> findByNameLike(String name);

	List<Restaurant> findByNameStartingWith(String string);

	List<Restaurant> findByNameEndingWith(String string);

	List<Restaurant> findByNameContaining(String string);

	List<Restaurant> findByNameNotContaining(String string);

	List<Restaurant> findByNameIn(Iterable<String> candidates);

	List<Restaurant> findByNameNotIn(Iterable<String> candidates);

	List<Restaurant> findByNameMatchesRegex(String foobar);

	List<Restaurant> findByNameExists();

	List<Restaurant> findByHalalIsTrue();

	List<Restaurant> findByHalalIsFalse();

	List<Restaurant> findBySimilarRestaurantsDescriptionIsNull();

	List<Restaurant> findByRegularDinersLastNameIsNull();

	List<Restaurant> findByNameNotContainingOrDescriptionIsNull(String nameContaining);

}
