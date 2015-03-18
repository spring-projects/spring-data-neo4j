package org.springframework.data.neo4j.integration.helloworld.domain;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

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
