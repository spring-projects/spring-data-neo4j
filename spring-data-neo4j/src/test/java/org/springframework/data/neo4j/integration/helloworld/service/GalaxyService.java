/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.helloworld.service;

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.integration.helloworld.domain.World;
import org.springframework.data.neo4j.integration.helloworld.repo.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Vince Bickers
 */
@Service
@Transactional
public class GalaxyService {

    @Autowired
    private WorldRepository worldRepository;

    @Autowired
    Session session;

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

    public World findWorldByName(String name) {
        Iterable<World> worlds = findByProperty("name", name);
        if (worlds.iterator().hasNext()) {
            return worlds.iterator().next();
        } else {
            return null;
        }
    }

    public Iterable<World> findAllByNumberOfMoons(int numberOfMoons) {
        return findByProperty("moons", numberOfMoons);
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

    private Iterable<World> findByProperty(String propertyName, Object propertyValue) {
        return session.loadAll(World.class, new Filter(propertyName, propertyValue));
    }

    public Iterable<World> findByProperty(String propertyName, Object propertyValue, int depth) {
        return session.loadAll(World.class, new Filter(propertyName, propertyValue), depth);
    }


    public Iterable<World> findAllWorlds(Pagination paging) {
        return session.loadAll(World.class, paging, 0);

    }

    public Iterable<World> findAllWorlds(Sort sort) {
        return worldRepository.findAll(sort, 0);

    }

    public Page<World> findAllWorlds(Pageable pageable) {
        return worldRepository.findAll(pageable, 0);
    }

    public Iterable<World> findAllWorlds(Sort sort, int depth) {
        return worldRepository.findAll(sort, depth);
    }
}
