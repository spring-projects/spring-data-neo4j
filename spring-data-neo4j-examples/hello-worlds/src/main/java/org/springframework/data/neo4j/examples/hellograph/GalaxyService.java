package org.springframework.data.neo4j.examples.hellograph;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GalaxyService {
	@Autowired
	private WorldRepository worldRepository;
	
	@Autowired
	private GalaxyMapper mapper;
	
    @Transactional
    public void makeSureGalaxyIsNotEmpty() {    	    	
        // Solar worlds
        resolveWorld("Mercury", 0);
        resolveWorld("Venus", 0);
        
        World earth = resolveWorld("Earth", 1);        
        World mars = resolveWorld("Mars", 2);        
        mars.addRocketRouteTo(earth);
        worldRepository.save(mars);
        
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
    	World createdWorld = worldRepository.findByPropertyValue("name", name);
    	if (createdWorld == null) {
    		createdWorld = new World(name, moons);
    		createdWorld = worldRepository.save(createdWorld);    		 
    	}
    	
    	return createdWorld;
    }

    public WorldDto findWorldNamed(String name) {
    	World world = worldRepository.findByPropertyValue("name", name);
        return mapper.worldDtoFromWorld(world);
    }
    
    public List<WorldDto> findReachableWorlds(Long worldId) {
    	World world = worldRepository.findOne(worldId);
    	return mapper.worldDtosFromWorlds(world.getReachableWorlds());
    }
    
    public List<WorldDto> findAllWorlds() {
    	Iterable<World> worlds = worldRepository.findAll();
    	return mapper.worldDtosFromWorlds(worlds);
    }
}
