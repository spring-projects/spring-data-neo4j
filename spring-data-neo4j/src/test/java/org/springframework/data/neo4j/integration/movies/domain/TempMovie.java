package org.springframework.data.neo4j.integration.movies.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

//todo merge with movie when tests fixed
@NodeEntity(label = "Movie")
public class TempMovie extends AbstractEntity {

    private String title;
    @Relationship(type = "RATED", direction = Relationship.INCOMING)
    private Set<Rating> ratings = new HashSet<>();

    public TempMovie() {
    }

    public TempMovie(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void addRating(Rating rating) {
        ratings.add(rating);
    }

    public Set<Rating> getRatings() {
        return ratings;
    }
}
