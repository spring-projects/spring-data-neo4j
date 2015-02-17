package org.springframework.data.neo4j.integration.repositories.domain;


public class Movie {

    private Long id;
    private String title;

    public Movie() {
    }

    public Movie(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
