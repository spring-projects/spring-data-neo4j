/*
 * Copyright 2011-2021 the original author or authors.
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
