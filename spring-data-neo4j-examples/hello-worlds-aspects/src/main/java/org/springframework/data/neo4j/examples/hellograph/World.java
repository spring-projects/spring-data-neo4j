package org.springframework.data.neo4j.examples.hellograph;


import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

@NodeEntity
public class World {
    @Indexed
    private String name;

    private int moons;

    @RelatedTo(type = RelationshipTypes.REACHABLE_BY_ROCKET, elementClass = World.class, direction = Direction.BOTH)
    private Set<World> reachableByRocket;

    public World(String name, int moons) {
        this.name = name;
        this.moons = moons;
    }

    public World() {
    }

    public String getName() {
        return name;
    }

    public int getMoons() {
        return moons;
    }

    @Override
    public String toString() {
        return String.format("World{name='%s', moons=%d}", name, moons);
    }

    public void addRocketRouteTo(World otherWorld) {
        relateTo(otherWorld, RelationshipTypes.REACHABLE_BY_ROCKET);
    }

    public Iterable<World> getReachableWorlds() {
    	return reachableByRocket;
    }
}
