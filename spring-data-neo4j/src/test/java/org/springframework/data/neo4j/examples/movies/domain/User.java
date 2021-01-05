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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Gerrit Meier
 */
public class User extends Person {

	private String middleName;
	private String surname;

	private Collection<Genre> interested = new HashSet<>();

	@Relationship(type = "FRIEND_OF",
			direction = Relationship.UNDIRECTED) private Collection<User> friends = new HashSet<>();

	@Relationship(type = "RATED") private Set<Rating> ratings = new HashSet<>();

	private Set<String> emailAddresses;

	public User() {}

	public User(String name) {
		setName(name);
	}

	public User(String name, String surname) {
		setName(name);
		this.surname = surname;
	}

	public void interestedIn(Genre genre) {
		interested.add(genre);
	}

	public void notInterestedIn(Genre genre) {
		interested.remove(genre);
	}

	public void befriend(User user) {
		friends.add(user);
		user.friends.add(this);
	}

	public Rating rate(TempMovie movie, int stars, String comment) {
		Rating rating = new Rating(this, movie, stars, comment);
		movie.addRating(rating);
		ratings.add(rating);
		return rating;
	}

	public Collection<User> getFriends() {
		return friends;
	}

	public String getMiddleName() {
		return middleName;
	}

	public Set<Rating> getRatings() {
		return ratings;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getSurname() {
		return surname;
	}

	public Set<String> getEmailAddresses() {
		return emailAddresses;
	}

	public void setEmailAddresses(Set<String> emailAddresses) {
		this.emailAddresses = emailAddresses;
	}
}
