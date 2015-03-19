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

package org.springframework.data.neo4j.integration.helloworld.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Vince Bickers
 */
public class World {

    private final static String REACHABLE_BY_ROCKET = "REACHABLE_BY_ROCKET";

    private Long id;

    private String name;

    private int moons;

    private Set<World> reachableByRocket = new HashSet<>();

    public World(String name, int moons) {
        this.name = name;
        this.moons = moons;
    }

    public World() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMoons() {
        return moons;
    }

    public void addRocketRouteTo(World otherWorld) {
        reachableByRocket.add(otherWorld);
        // symmetric relationship.
        otherWorld.reachableByRocket.add(this); // bi-directional in domain.
    }

    @Relationship(type=REACHABLE_BY_ROCKET)
    public Set<World> getReachableByRocket() {
        return this.reachableByRocket;
    }

    @Relationship(type=REACHABLE_BY_ROCKET)
    public void setReachableByRocket(Set<World> reachableByRocket) {
        this.reachableByRocket.clear();
        this.reachableByRocket = reachableByRocket;
    }

    public boolean canBeReachedFrom(World otherWorld) {
        return reachableByRocket.contains(otherWorld);
    }

    @Override
    public int hashCode() {
        return (id == null) ? 0 : id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        World other = (World) obj;
        if (id == null) return other.id == null;
        return id.equals(other.id);
    }

    @Override
    public String toString() {
        return String.format("World{name='%s', moons=%d}", name, moons);
    }
}
