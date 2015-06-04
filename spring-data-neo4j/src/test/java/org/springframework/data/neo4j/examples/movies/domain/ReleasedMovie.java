/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.examples.movies.domain;

import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.typeconversion.DateLong;

import java.util.Date;

/**
 * @author Michal Bachman
 */
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
