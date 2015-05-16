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

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * @author Michal Bachman
 */
@RelationshipEntity(type = "RATED")
public class Rating {
    private Long id;

    @StartNode
    private User user;
    @EndNode
    private TempMovie movie;
    private int stars;
    private String comment;

    public Rating() {}

    public Rating(User user, TempMovie movie, int stars, String comment) {
        this.user = user;
        this.movie = movie;
        this.stars = stars;
        this.comment = comment;
    }

    public Long getId() {
        return id;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setMovie(TempMovie movie) {
        this.movie = movie;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public User getUser() {
        return user;
    }

    public TempMovie getMovie() {
        return movie;
    }

    public int getStars() {
        return stars;
    }

    public String getComment() {
        return comment;
    }
}
