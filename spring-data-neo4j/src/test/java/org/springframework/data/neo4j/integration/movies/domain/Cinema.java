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

import java.util.HashSet;
import java.util.Set;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Property;
import org.neo4j.ogm.annotation.Relationship;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
@NodeEntity(label = "Theatre")
public class Cinema {

    private Long id;
    private String name;

    @Property(name = "city")
    private String location;

    @Relationship(direction = Relationship.INCOMING)
    private Set<User> visited = new HashSet<>();

    private int capacity;

    public Cinema() {
    }

    public Cinema(String name) {
        this.name = name;
    }

    public Cinema(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public void addVisitor(User user) {
        visited.add(user);
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public Set<User> getVisited() {
        return visited;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Cinema cinema = (Cinema) o;

        return !(name != null ? !name.equals(cinema.name) : cinema.name != null);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
