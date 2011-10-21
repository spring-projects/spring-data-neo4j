package org.springframework.data.neo4j.examples.hellograph;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.Traversal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

import static org.neo4j.graphdb.DynamicRelationshipType.withName;
import static org.springframework.data.neo4j.examples.hellograph.RelationshipTypes.REACHABLE_BY_ROCKET;

/**
 * Spring Data Neo4j backed application context for Worlds.
 */
public class WorldRepositoryImpl implements MyWorldRepository {

    @Autowired private WorldRepository worldRepository;

    @Override
    @Transactional
    public Collection<World> makeSomeWorlds() {
        ArrayList<World> newWorlds = new ArrayList<World>();

        // solar worlds
        newWorlds.add(world("Mercury", 0));
        newWorlds.add(world("Venus", 0));
        World earth = world("Earth", 1);
        newWorlds.add(earth);
        
        World mars = world("Mars", 2);
        mars.addRocketRouteTo(earth);
        worldRepository.save(mars);
        
        newWorlds.add(mars);
        newWorlds.add(world("Jupiter", 63));
        newWorlds.add(world("Saturn", 62));
        newWorlds.add(world("Uranus", 27));
        newWorlds.add(world("Neptune", 13));

        // Norse worlds
        newWorlds.add(world("Alfheimr", 0));
        newWorlds.add(world("Midgard", 1));
        newWorlds.add(world("Muspellheim", 2));
        newWorlds.add(world("Asgard", 63));
        newWorlds.add(world("Hel", 62));
        
        worldRepository.save(newWorlds);

        return newWorlds;
    }


    @Override
    @Transactional
    public World world(String name, int moons) {
        World createdWorld = new World(name, moons);
    	worldRepository.save(createdWorld);
    	return createdWorld;
    }

    @Override
    public World findWorldNamed(String name) {
        return worldRepository.findByPropertyValue("name", name);
    }

    @Override
    public Iterable<World> findWorldsWithMoons(int moonCount) {
        return worldRepository.findAllByPropertyValue("moon-index", "moons", moonCount);
    }

    @Override
    public Iterable<World> exploreWorldsBeyond(World homeWorld) {
        TraversalDescription traversal = Traversal.description().relationships(withName(REACHABLE_BY_ROCKET), Direction.OUTGOING);
        return worldRepository.findAllByTraversal(homeWorld, traversal);
    }

}
