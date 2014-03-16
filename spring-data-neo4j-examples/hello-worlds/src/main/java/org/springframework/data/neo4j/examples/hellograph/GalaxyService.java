package org.springframework.data.neo4j.examples.hellograph;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.hellograph.domain.World;
import org.springframework.data.neo4j.examples.hellograph.repositories.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

@Service
@Transactional
public class GalaxyService {
	
	@Autowired
	private WorldRepository worldRepository;
	
	public long getNumberOfWorlds() {
		return worldRepository.count();
	}
	
	public World createWorld(String name, int moons) {
		return worldRepository.save(new World(name, moons));
	}
	
	public Iterable<World> getAllWorlds() {
		return worldRepository.findAll();
	}
	
	public World findWorldById(Long id) {
		return worldRepository.findOne(id);
	}

    // This is using the schema based index
	public World findWorldByName(String name) {
		return worldRepository.findBySchemaPropertyValue("name", name);
	}

    // This is using the legacy index
	public Iterable<World> findAllByNumberOfMoons(int numberOfMoons) {
		return worldRepository.findAllByPropertyValue("moons", numberOfMoons);
	}
	
	public Collection<World> makeSomeWorlds() {
		Collection<World> worlds = new ArrayList<World>();
				
		// Solar worlds		
		worlds.add(createWorld("Mercury", 0));
		worlds.add(createWorld("Venus", 0));
        
        World earth = createWorld("Earth", 1);        
        World mars = createWorld("Mars", 2);        
        mars.addRocketRouteTo(earth);
        worldRepository.save(mars);
        worlds.add(earth);
        worlds.add(mars);
        
        worlds.add(createWorld("Jupiter", 63));
        worlds.add(createWorld("Saturn", 62));
        worlds.add(createWorld("Uranus", 27));
        worlds.add(createWorld("Neptune", 13));

        // Norse worlds
        worlds.add(createWorld("Alfheimr", 0));
        worlds.add(createWorld("Midgard", 1));
        worlds.add(createWorld("Muspellheim", 2));
        worlds.add(createWorld("Asgard", 63));
        worlds.add(createWorld("Hel", 62));
        
        return worlds;
	}
	
}
