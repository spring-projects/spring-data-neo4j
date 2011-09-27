package org.neo4j.examples.imdb.domain;

import java.util.List;

public interface ImdbService {
    /**
     * Store a new actor in the graph and add the name to the index.
     *
     * @param name
     * @return the new actor
     */
    Actor createActor(String name);

    /**
     * Store a new movie and add the title to the index.
     *
     * @param title title of the movie
     * @param year  year of release
     * @return the new movie
     */
    Movie createMovie(String title, int year);

    /**
     * Returns the actor with the given <code>name</code> or <code>null</code>
     * if not found.
     *
     * @param name name of actor
     * @return actor or <code>null</code> if not found
     */
    Actor getActor(String name);

    /**
     * Return the movie with given <code>title</code> or <code>null</code> if
     * not found.
     *
     * @param title movie title
     * @return movie or <code>null</code> if not found
     */
    Movie getMovie(String title);

    Movie getExactMovie(String title);

    /**
     * Returns a list with first element {@link Actor} followed by {@link Movie}
     * ending with an {@link Actor}. The list is one of the shortest paths
     * between the <code>actor</code> and actor Kevin Bacon.
     *
     * @param actor name of actor to find shortest path to Kevin Bacon
     * @return one of the shortest paths to Kevin Bacon
     */
    List<?> getBaconPath(Actor actor);

    /**
     * Add a relationship from some node to the reference node.
     * Will make it easy and fast to retrieve this node.
     */
    void setupReferenceRelationship();
}
