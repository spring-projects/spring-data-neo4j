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

package org.springframework.data.neo4j.integration.movies.domain;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michal Bachman
 */
//todo merge with movie when tests fixed
@NodeEntity(label = "Movie")
public class TempMovie extends AbstractEntity {

    private String title;
    @Relationship(type = "RATED", direction = Relationship.INCOMING)
    private Set<Rating> ratings = new HashSet<>();

    public TempMovie() {
    }

    public TempMovie(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void addRating(Rating rating) {
        ratings.add(rating);
    }

    public Set<Rating> getRatings() {
        return ratings;
    }
}
