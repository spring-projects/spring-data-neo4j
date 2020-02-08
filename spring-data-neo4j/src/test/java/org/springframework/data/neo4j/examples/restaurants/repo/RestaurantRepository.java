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
package org.springframework.data.neo4j.examples.restaurants.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Range;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.annotation.ExistsQuery;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Jasper Blues
 */
public interface RestaurantRepository extends Neo4jRepository<Restaurant, Long> {

	List<Restaurant> findByNameAndLocationNear(String name, Distance distance, Point point);

	List<Restaurant> findByLocationNearAndName(Distance distance, Point point, String name);

	List<Restaurant> findByScoreBetween(double min, double max);

	List<Restaurant> findByScoreBetween(Range range);

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

	boolean existsByDescription(String description);

	@Query(value = "MATCH (r:Restaurant) WHERE r.description = 'good' return r", exists = true)
	boolean existenceOfAGoodRestaurant();

	@ExistsQuery("MATCH (r:Restaurant) WHERE r.description = 'good' return r")
	boolean existenceOfAGoodRestaurantWithExistsQuery();
}
