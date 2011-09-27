package org.springframework.data.neo4j.examples.hellograph;


import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.Set;

/**
 * A Spring Data Neo4j enhanced World entity.
 * <p/>
 * This is the initial POJO in the Universe.
 */
@NodeEntity
public class World
{
    @Indexed
    private String name;

    @Indexed(indexName = "moon-index")
    private int moons;

    @RelatedTo(type = "REACHABLE_BY_ROCKET", elementClass = World.class, direction = Direction.BOTH)
    private Set<World> reachableByRocket;

    public World( String name, int moons )
    {
        this.name = name;
        this.moons = moons;
    }

    public World()
    {
    }

    public String getName()
    {
        return name;
    }

    public int getMoons()
    {
        return moons;
    }

    @Override
    public String toString()
    {
        return String.format("World{name='%s, moons=%d}", name, moons);
    }

    public void addRocketRouteTo( World otherWorld )
    {
        relateTo( otherWorld, RelationshipTypes.REACHABLE_BY_ROCKET );
    }

    public boolean canBeReachedFrom( World otherWorld )
    {
        return reachableByRocket.contains( otherWorld );
    }
}
