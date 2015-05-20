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

package org.springframework.data.neo4j.integration.helloworld;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.cypher.query.Pagination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.integration.helloworld.context.HelloWorldContext;
import org.springframework.data.neo4j.integration.helloworld.domain.World;
import org.springframework.data.neo4j.integration.helloworld.service.GalaxyService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;


/**
 * @author Vince Bickers
 */
@ContextConfiguration(classes = {HelloWorldContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class GalaxyServiceTest {

    @Autowired
    private GalaxyService galaxyService;

    @Before
    public void setUp() {
        galaxyService.deleteAll();
        assertEquals(0, galaxyService.getNumberOfWorlds());
    }

    @Test
    public void shouldAllowDirectWorldCreation() {

        World myWorld = galaxyService.createWorld("mine", 0);
        Collection<World> foundWorlds = (Collection<World>) galaxyService.getAllWorlds();

        assertEquals(1, foundWorlds.size());
        World mine = foundWorlds.iterator().next();

        assertEquals(myWorld.getName(), mine.getName());

    }

    @Test
    public void shouldHaveCorrectNumberOfWorlds() {
        galaxyService.makeSomeWorlds();
        assertEquals(13, galaxyService.getNumberOfWorlds());
    }

    @Test
    public void createAllWorldsAtOnce() {
        galaxyService.makeAllWorldsAtOnce();
        assertEquals(13, galaxyService.getNumberOfWorlds());

        World earth = galaxyService.findWorldByName("Earth");
        World mars = galaxyService.findWorldByName("Mars");

        assertTrue(mars.canBeReachedFrom(earth));
        assertTrue(earth.canBeReachedFrom(mars));
    }

    @Test
    public void shouldFindWorldsById() {
        galaxyService.makeSomeWorlds();

        for (World world : galaxyService.getAllWorlds()) {
            World foundWorld = galaxyService.findWorldById(world.getId());
            assertNotNull(foundWorld);
        }
    }


    @Test
    public void shouldFindWorldsByName() {
        galaxyService.makeSomeWorlds();
        for (World world : galaxyService.getAllWorlds()) {
            World foundWorld = galaxyService.findWorldByName(world.getName());
            assertNotNull(foundWorld);
        }
    }

    @Test
    public void shouldReachMarsFromEarth() {
        galaxyService.makeSomeWorlds();

        World earth = galaxyService.findWorldByName("Earth");
        World mars = galaxyService.findWorldByName("Mars");

        assertTrue(mars.canBeReachedFrom(earth));
        assertTrue(earth.canBeReachedFrom(mars));
    }

    @Test
    public void shouldFindAllWorlds() {

        Collection<World> madeWorlds = galaxyService.makeSomeWorlds();
        Iterable<World> foundWorlds = galaxyService.getAllWorlds();

        int countOfFoundWorlds = 0;
        for (World foundWorld : foundWorlds) {
            assertTrue(madeWorlds.contains(foundWorld));
            countOfFoundWorlds++;
        }

        assertEquals(madeWorlds.size(), countOfFoundWorlds);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldFindWorldsWith1Moon() {
        galaxyService.makeSomeWorlds();

        for (World worldWithOneMoon : galaxyService.findAllByNumberOfMoons(1)) {
            assertThat(
                    worldWithOneMoon.getName(),
                    is(anyOf(containsString("Earth"), containsString("Midgard"))));
        }
    }

    @Test
    public void shouldNotFindKrypton() {
        galaxyService.makeSomeWorlds();
        World krypton = galaxyService.findWorldByName("Krypton");
        assertNull(krypton);
    }

    @Test
    public void shouldSupportPaging() {

        List<World>  worlds = (List<World>) galaxyService.makeAllWorldsAtOnce();

        int count = worlds.size();
        int PAGE_SIZE = 3;
        int pages = count / PAGE_SIZE + 1;
        long n = 0;
        for (World world : worlds) {
            n += world.getId();
        }

        for (int page = 0; page < pages; page++) {
            Iterable<World> paged = galaxyService.findAllWorlds(new Pagination(page, PAGE_SIZE));
            for (World world : paged) {
                System.out.println(world.getName() + ":" + world.getId());
                n -= world.getId();
            }
        }

        assertEquals(0L, n);

    }

    @Test
    public void shouldDetectNotOnLastPage() {

        int count = galaxyService.makeAllWorldsAtOnce().size();

        assertEquals(count, 13);

        Pageable pageable = new PageRequest(2, 3);
        Page<World> worlds = galaxyService.findAllWorlds(pageable);


        for ( World world : worlds) {
            System.out.println(world.getName() + ": " + world.getId());
        }
        assertTrue(worlds.hasNext());

    }

    @Test
    public void shouldDetectLastPage() {

        int count = galaxyService.makeAllWorldsAtOnce().size();

        assertEquals(count, 13);

        Pageable pageable = new PageRequest(4, 3);
        Page<World> worlds = galaxyService.findAllWorlds(pageable);


        for ( World world : worlds) {
            System.out.println(world.getName() + ": " + world.getId());
        }
        assertFalse(worlds.hasNext());

    }

    @Test
    public void shouldIterateAllWorldsUsingPageable() {

        int count = galaxyService.makeAllWorldsAtOnce().size();

        assertEquals(count, 13);

        Pageable pageable = new PageRequest(0, 3);

        for(;;) {
            Page<World> worlds = galaxyService.findAllWorlds(pageable);
            for ( World world : worlds) {
                System.out.println(world.getName() + ": " + world.getId());
                count--;
            }
            if (!worlds.hasNext()) {
                break;
            }
            pageable = pageable.next();
        }

        assertEquals(0, count);
    }

    @Test
    public void shouldPageAllWorldsSorted() {

        int count = galaxyService.makeAllWorldsAtOnce().size();

        assertEquals(count, 13);

        Pageable pageable = new PageRequest(0, 3, Sort.Direction.ASC, "name");

        for(;;) {
            Page<World> worlds = galaxyService.findAllWorlds(pageable);
            for ( World world : worlds) {
                System.out.println(world.getName() + ": " + world.getId());
                count--;
            }
            if (!worlds.hasNext()) {
                break;
            }
            pageable = pageable.next();
        }

        assertEquals(0, count);
    }

    @Test
    public void shouldIterateAllWorldsSorted() {

        int count = galaxyService.makeAllWorldsAtOnce().size();

        assertEquals(count, 13);

        Sort sort = new Sort(Sort.Direction.ASC, "name");

        for (World world : galaxyService.findAllWorlds(sort)) {
            System.out.println(world.getName() + ": " + world.getId());
            count--;
        }

        assertEquals(0, count);
    }

}
