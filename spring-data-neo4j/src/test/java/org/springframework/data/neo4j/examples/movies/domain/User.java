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

package org.springframework.data.neo4j.examples.movies.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Michal Bachman
 */
public class User extends Person{

    private String middleName;
    private Collection<Genre> interested = new HashSet<>();

    @Relationship(type = "FRIEND_OF", direction = Relationship.UNDIRECTED)
    private Collection<User> friends = new HashSet<>();

    @Relationship(type = "RATED")
    private Set<Rating> ratings = new HashSet<>();

    public User() {
    }

    public User(String name) {
        setName(name);
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

    public String getMiddleName()
    {
        return middleName;
    }
}
