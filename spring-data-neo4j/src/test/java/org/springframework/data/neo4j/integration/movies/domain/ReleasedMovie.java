package org.springframework.data.neo4j.integration.movies.domain;

import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

import java.util.Date;

//the fields here will move to Movie, this separate class exists temporarily after its tests pass
public class ReleasedMovie extends AbstractAnnotatedEntity {

    private String title;

    @Property(name = "cinemaRelease")
    private Date released;

    @DateLong
    private Date cannesRelease;

    public ReleasedMovie() {
    }

    public ReleasedMovie(String title, Date released, Date cannesRelease) {
        this.title = title;
        this.released = released;
        this.cannesRelease = cannesRelease;
    }

    public String getTitle() {
        return title;
    }

    public Date getReleased() {
        return released;
    }

    public Date getCannesRelease() {
        return cannesRelease;
    }
}
