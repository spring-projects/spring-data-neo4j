package org.neo4j.examples.imdb.web;

import org.neo4j.examples.imdb.domain.Movie;
import org.neo4j.examples.imdb.domain.Role;

/**
* @author mh
* @since 01.04.11
*/
public final class MovieInfo implements Comparable<MovieInfo> {
    private String title;
    private String role;

    MovieInfo(final Movie movie, final Role role) {
        setTitle(movie.getTitle());
        if (role == null || role.getName() == null) {
            setRole("(unknown)");
        } else {
            setRole(role.getName());
        }
    }

    public final void setTitle(final String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public final void setRole(final String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public int compareTo(final MovieInfo otherMovieInfo) {
        return getTitle().compareTo(otherMovieInfo.getTitle());
    }
}
