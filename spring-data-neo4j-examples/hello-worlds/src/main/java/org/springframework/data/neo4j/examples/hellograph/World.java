package org.springframework.data.neo4j.examples.hellograph;


import org.neo4j.graphdb.Direction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * A Spring Data Neo4j enhanced World entity.
 * <p/>
 * This is the initial POJO in the Universe.
 */
@NodeEntity
public class World 
{   
    @GraphId Long id;
    
    @Indexed
    private String name;

    @Indexed(indexName = "moon-index")
    private int moons;

    @RelatedTo(type = "REACHABLE_BY_ROCKET", elementClass = World.class, direction = Direction.BOTH)
    private Set<World> reachableByRocket = new HashSet<World>();

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
    	reachableByRocket.add(otherWorld);
    }

    public boolean canBeReachedFrom( World otherWorld )
    {
        return reachableByRocket.contains( otherWorld );
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		World other = (World) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
}
