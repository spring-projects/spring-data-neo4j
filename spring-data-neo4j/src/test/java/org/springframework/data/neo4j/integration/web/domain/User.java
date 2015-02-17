package org.springframework.data.neo4j.integration.web.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.Collection;
import java.util.HashSet;

public class User {

    private Long id;
    private String name;
    private Collection<Genre> interested = new HashSet<>();

    @Relationship(type = "FRIEND_OF", direction = Relationship.UNDIRECTED)
    private Collection<User> friends = new HashSet<>();

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
}
