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
package org.springframework.data.neo4j.examples.movies.domain.queryresult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.neo4j.annotation.QueryResult;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;

/**
 * {@link QueryResult} that wraps entity objects.
 *
 * @author Luanne Misquitta
 */
@QueryResult
public class EntityWrappingQueryResult {

	private User user;
	private Set<User> friends;
	private List<Rating> ratings;
	private TempMovie[] movies;
	private float avgRating;
	private List<Float> allRatings;
	private Collection<Map<String, Object>> literalMap;

	public User getUser() {
		return user;
	}

	public Set<User> getFriends() {
		return friends;
	}

	public List<Rating> getRatings() {
		return ratings;
	}

	public float getAvgRating() {
		return avgRating;
	}

	public TempMovie[] getMovies() {
		return movies;
	}

	public List<Float> getAllRatings() {
		return allRatings;
	}

	public Collection<Map<String, Object>> getLiteralMap() {
		return literalMap;
	}

}
