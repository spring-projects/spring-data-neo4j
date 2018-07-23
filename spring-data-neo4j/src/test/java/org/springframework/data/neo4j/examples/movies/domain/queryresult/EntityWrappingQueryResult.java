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
