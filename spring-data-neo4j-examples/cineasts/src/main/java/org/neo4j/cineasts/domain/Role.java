package org.neo4j.cineasts.domain;

import org.springframework.data.graph.annotation.EndNode;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.annotation.StartNode;

/**
 * @author mh
 * @since 04.03.11
 */
@RelationshipEntity
public class Role {
    @EndNode Movie movie;
    @StartNode Person actor;

    String name;

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
