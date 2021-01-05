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
package org.springframework.data.neo4j.examples.movies.domain;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * @author Michal Bachman
 */
@RelationshipEntity(type = "RATED")
public class Rating implements Comparable {
	private Long id;

	@StartNode private User user;
	@EndNode private TempMovie movie;
	private int stars;
	private String comment;

	private long ratingTimestamp;

	public Rating() {}

	public Rating(User user, TempMovie movie, int stars, String comment) {
		this.user = user;
		this.movie = movie;
		this.stars = stars;
		this.comment = comment;
	}

	public Long getId() {
		return id;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setMovie(TempMovie movie) {
		this.movie = movie;
	}

	public void setStars(int stars) {
		this.stars = stars;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public User getUser() {
		return user;
	}

	public TempMovie getMovie() {
		return movie;
	}

	public int getStars() {
		return stars;
	}

	public String getComment() {
		return comment;
	}

	public long getRatingTimestamp() {
		return ratingTimestamp;
	}

	public void setRatingTimestamp(long ratingTimestamp) {
		this.ratingTimestamp = ratingTimestamp;
	}

	@Override
	public int compareTo(Object o) {
		Rating other = (Rating) o;
		if (stars == ((Rating) o).getStars()) {
			return getUser().getName().compareTo(other.getUser().getName());
		}
		return stars - other.getStars();
	}
}
