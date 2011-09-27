package org.neo4j.examples.imdb.domain;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

/**
 * @author mh
 * @since 16.03.11
 */
@NodeEntity
public class Lookup {
    private
    @Indexed
    String word;
    private int count;

    @RelatedTo(elementClass = Movie.class, type = "title.part")
    private Set<Movie> movies;
    @RelatedTo(elementClass = Actor.class, type = "name.part")
    private Set<Actor> actors;

    public Lookup(String word) {
        this.word = word;
    }

    public Lookup() {
    }

    public void addActor(Actor actor) {
        relateTo(actor, "name.part");
        count++;
    }

    public void addMovie(Movie movie) {
        this.movies.add(movie);
        count++;
    }

    public String getWord() {
        return word;
    }

    public int getCount() {
        return count;
    }

    public Set<Movie> getMovies() {
        return movies;
    }

    public Set<Actor> getActors() {
        return actors;
    }

    public Movie getMovie() {
        Set<Movie> allMovies = getMovies();
        return allMovies != null && !allMovies.isEmpty() ? allMovies.iterator().next() : null;
    }

    public Actor getActor() {
        Set<Actor> allActors = getActors();
        return allActors != null && !allActors.isEmpty() ? allActors.iterator().next() : null;
    }
}
