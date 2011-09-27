package org.neo4j.examples.imdb.domain;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

@RelationshipEntity
public class Role {
    String role;
    @StartNode
    Actor actor;
    @EndNode
    Movie movie;

    @Override
    public String toString() {
        return String.format("%s-[%s]->%s", this.getActor(), role, this.getMovie());
    }

    public Actor getActor() {
        return actor;
    }

    public Movie getMovie() {
        return movie;
    }

    public String getName() {
        return role;
    }

    public Role play(String name) {
        this.role = name;
        return this;
    }
}
