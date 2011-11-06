package org.neo4j.cineasts.domain;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

/**
 * @author mh
 * @since 04.03.11
 */
@RelationshipEntity(type = "ACTS_IN")
public class Role {
    @EndNode Movie movie;
    @StartNode Person actor;

    String name;

    public Role() {
    }

    public Role(Movie movie, Person actor, String roleName) {
        this.movie = movie;
        this.actor = actor;
        this.name = roleName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Movie getMovie() {
        return movie;
    }

    public Person getActor() {
        return actor;
    }

    @Override
    public String toString() {
        return String.format("%s acts as %s in %s", actor, name, movie);
    }
}
