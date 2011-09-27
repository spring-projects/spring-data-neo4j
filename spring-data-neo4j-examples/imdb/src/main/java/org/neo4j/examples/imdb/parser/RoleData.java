package org.neo4j.examples.imdb.parser;

/**
 * Holds information about what role an actor has in a movie
 */
public class RoleData {
    private final String title;
    private final String role;

    RoleData(final String title, final String role) {
        this.title = title;
        this.role = role;
    }

    /**
     * Returns the title of the movie, never <code>null</code>.
     *
     * @return title of the movie
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Returns the role the actor had in the movie, may be <code>null</code>
     * if no information is available.
     *
     * @return actor role or null if information not avilable
     */
    public String getRole() {
        return this.role;
    }
}
