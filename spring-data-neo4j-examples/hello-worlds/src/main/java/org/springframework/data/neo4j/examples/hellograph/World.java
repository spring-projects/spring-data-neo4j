package org.springframework.data.neo4j.examples.hellograph;


import org.neo4j.graphdb.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Set;

/**
 * A Spring Data Neo4j enhanced World entity.
 * <p/>
 * This is the initial POJO in the Universe.
 */
@NodeEntity
public class World 
{
	@Autowired Neo4jTemplate template;
	
    @Autowired private WorldRepository worldRepository;
    
    @GraphId Long id;
    
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
    	template.createRelationshipBetween(this, otherWorld, null, RelationshipTypes.REACHABLE_BY_ROCKET, false);
    }

    public boolean canBeReachedFrom( World otherWorld )
    {
        return reachableByRocket.contains( otherWorld );
    }
}
