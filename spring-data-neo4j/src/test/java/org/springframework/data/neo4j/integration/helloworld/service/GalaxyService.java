package org.springframework.data.neo4j.integration.helloworld.service;

import org.springframework.data.neo4j.integration.helloworld.domain.World;
import org.springframework.data.neo4j.integration.helloworld.repo.WorldRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
        //return worldRepository.findBySchemaPropertyValue("name", name);
        Iterable<World> worlds = worldRepository.findByProperty("name", name);
        if (worlds.iterator().hasNext()) {
            return worlds.iterator().next();
        } else {
            return null;
        }
    }

    // This is using the legacy index
    public Iterable<World> findAllByNumberOfMoons(int numberOfMoons) {
        //return worldRepository.findAllByPropertyValue("moons", numberOfMoons);
        return worldRepository.findByProperty("moons", numberOfMoons);
    }

    public Collection<World> makeSomeWorlds() {

        Collection<World> worlds = new ArrayList<World>();

        // Solar worlds
        worlds.add(createWorld("Mercury", 0));
        worlds.add(createWorld("Venus", 0));

        World earth = createWorld("Earth", 1);
        World mars = createWorld("Mars", 2);

        mars.addRocketRouteTo(earth);

        // todo: handle bi-directional automatically
        //earth.addRocketRouteTo(mars);


        // this is a bit silly
        worldRepository.save(mars);

        // todo: handle-bidirectional automatically
        //worldRepository.save(earth);

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

    // not in original
    public Collection<World> makeAllWorldsAtOnce() {

        Collection<World> worlds = new ArrayList<World>();
        
        // Solar worlds
        
        worlds.add(new World("Mercury", 0));
        worlds.add(new World("Venus", 0));

        World earth = new World("Earth", 1);
        World mars = new World("Mars", 2);

        mars.addRocketRouteTo(earth);
        earth.addRocketRouteTo(mars);

        worlds.add(earth);
        worlds.add(mars);

        worlds.add(new World("Jupiter", 63));
        worlds.add(new World("Saturn", 62));
        worlds.add(new World("Uranus", 27));
        worlds.add(new World("Neptune", 13));

        // Norse worlds
        worlds.add(new World("Alfheimr", 0));
        worlds.add(new World("Midgard", 1));
        worlds.add(new World("Muspellheim", 2));
        worlds.add(new World("Asgard", 63));
        worlds.add(new World("Hel", 62));

        worldRepository.save(worlds);

        return worlds;
    }
    
    public void deleteAll() {
        worldRepository.deleteAll();
    }

}
