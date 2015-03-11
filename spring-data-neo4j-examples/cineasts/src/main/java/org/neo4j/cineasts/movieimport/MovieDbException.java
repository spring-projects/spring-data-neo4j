package org.neo4j.cineasts.movieimport;

public class MovieDbException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MovieDbException(String message) {
        super(message);
    }

    public MovieDbException(String message, Throwable cause) {
        super(message, cause);
    }
}
