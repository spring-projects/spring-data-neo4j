package org.springframework.data.neo4j.integration.helloworld;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.integration.helloworld.context.HelloWorldContext;
import org.springframework.data.neo4j.integration.helloworld.domain.World;
import org.springframework.data.neo4j.integration.helloworld.service.GalaxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;


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

}
