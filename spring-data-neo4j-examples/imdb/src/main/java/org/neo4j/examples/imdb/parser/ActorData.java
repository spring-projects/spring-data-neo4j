package org.neo4j.examples.imdb.parser;

public class ActorData {
    private final String name;
    private final RoleData[] movieRoles;

    /**
     * Create container for actor data.
     *
     * @param name       name of actor
     * @param movieRoles movie roles of actor
     */
    ActorData(final String name, final RoleData[] movieRoles) {
        this.movieRoles = movieRoles;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public RoleData[] getMovieRoles() {
        return movieRoles;
    }
}
