/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.domain.cineasts.annotated;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michal Bachman
 */
public class Movie {

    Long id;
    String title;
    int year;

    @Relationship(type="ACTS_IN", direction="INCOMING")
    Set<Role> roles=new HashSet<>();

    @Relationship(type = "RATED", direction = Relationship.INCOMING)
    Set<Rating> ratings;

    Set<Nomination> nominations;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Set<Rating> getRatings() {
        return ratings;
    }

    @Relationship(type="RATED", direction=Relationship.INCOMING)
    public void setRatings(Set<Rating> ratings) {
        this.ratings = ratings;
    }

    public Set<Nomination> getNominations() {
        return nominations;
    }

    public void setNominations(Set<Nomination> nominations) {
        this.nominations = nominations;
    }
}
