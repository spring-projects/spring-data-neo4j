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

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.repository.Neo4jRepository;

/**
 * @author Luanne Misquitta
 * @author Vince Bickers
 */
public interface RatingRepository extends Neo4jRepository<Rating, Long> {

	List<Rating> findByStars(int stars);

	List<Rating> findByStarsAndRatingTimestamp(int stars, long ratingTimestamp);

	List<Rating> findByStarsOrRatingTimestamp(int stars, long ratingTimestamp);

	List<Rating> findByStarsAndRatingTimestampLessThan(int stars, long ratingTimestamp);

	List<Rating> findByStarsOrRatingTimestampGreaterThan(int stars, long ratingTimestamp);

	List<Rating> findByUserName(String name);

	List<Rating> findByMovieName(String name);

	List<Rating> findByUserNameAndMovieName(String userName, String movieName);

	List<Rating> findByUserNameAndStars(String name, int stars);

	List<Rating> findByStarsAndMovieName(int stars, String name);

	List<Rating> findByUserNameAndMovieNameAndStars(String userName, String movieName, int stars);

	List<Rating> findByStarsOrUserName(int stars, String username);

	List<Rating> findByUserNameAndUserMiddleName(String username, String middleName);

	Long countByStars(int stars);

	Long removeByUserName(String username);

	List<Long> deleteByStarsOrRatingTimestampGreaterThan(int stars, long ratingTimestamp);

	@Query("MATCH (a)-[r:RATED]-(b) WHERE ID(a)={0} and ID(b)={1} RETURN a,r,b")
	Rating findRatingByUserAndTempMovie(long userId, long tempMovieId);
}
