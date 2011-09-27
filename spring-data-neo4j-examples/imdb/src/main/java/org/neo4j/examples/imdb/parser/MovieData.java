package org.neo4j.examples.imdb.parser;

public class MovieData {
    private final String title;
    private final int year;

    /**
     * Create container for movie data.
     *
     * @param title title of movie
     * @param year  release year of movie
     */
    MovieData(final String title, final int year) {
        this.title = title;
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year;
    }
}
