package org.springframework.data.neo4j.integration.movies.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class User {

    private Long id;
    private String name;
    private String middleName;
    private Collection<Genre> interested = new HashSet<>();

    @Relationship(type = "FRIEND_OF", direction = Relationship.UNDIRECTED)
    private Collection<User> friends = new HashSet<>();

    @Relationship(type = "RATED")
    private Set<Rating> ratings = new HashSet<>();

    public User() {
    }

    public User(String name) {
        this.name = name;
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

    //this doesn't need to be part of the domain, but this class is for testing
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Collection<User> getFriends() {
        return friends;
    }

    public String getMiddleName()
    {
        return middleName;
    }
}
