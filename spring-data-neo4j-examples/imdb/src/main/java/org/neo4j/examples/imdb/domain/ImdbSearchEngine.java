package org.neo4j.examples.imdb.domain;

public interface ImdbSearchEngine {
    void indexActor(Actor actor);

    void indexMovie(Movie movie);

    Actor searchActor(String name);

    Movie searchMovie(String title);
}
