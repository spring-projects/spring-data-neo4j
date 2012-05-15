package org.springframework.data.neo4j.examples.hellograph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data Neo4j backed application context for Worlds.
 */
public class WorldRepositoryImpl implements MyWorldRepository {

    @Autowired
    private WorldRepository worldRepository;

    @Override
    @Transactional
    public void makeSureGalaxyIsNotEmpty() {
        // Solar worlds
        resolveWorld("Mercury", 0);
        resolveWorld("Venus", 0);
        
        World earth = resolveWorld("Earth", 1);        
        World mars = resolveWorld("Mars", 2);        
        mars.addRocketRouteTo(earth);
        
        resolveWorld("Jupiter", 63);
        resolveWorld("Saturn", 62);
        resolveWorld("Uranus", 27);
        resolveWorld("Neptune", 13);

        // Norse worlds
        resolveWorld("Alfheimr", 0);
        resolveWorld("Midgard", 1);
        resolveWorld("Muspellheim", 2);
        resolveWorld("Asgard", 63);
        resolveWorld("Hel", 62);        
    }
    
    private World resolveWorld(String name, int moons) {
    	World createdWorld = findWorldNamed(name);
    	if (createdWorld == null) {
    		createdWorld = new World(name, moons);
    		worldRepository.save(createdWorld);
    	}
    	return createdWorld;
    }

    @Override
    public World findWorldNamed(String name) {
        return worldRepository.findByPropertyValue("name", name);
    }
    
    @Override
    public Iterable<World> findAllWorlds() {
    	return worldRepository.findAll();
    }
}
