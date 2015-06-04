package org.springframework.data.neo4j.queries;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.result.QueryStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.galaxy.context.GalaxyContext;
import org.springframework.data.neo4j.examples.galaxy.domain.World;
import org.springframework.data.neo4j.examples.galaxy.repo.WorldRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vince Bickers
 */
@ContextConfiguration(classes = {GalaxyContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class QueryReturnTypesTest {

    @Autowired
    WorldRepository worldRepository;

    @Test
    public void shouldCallExecuteWhenPrimitiveVoidReturnTypeOnQuery() {

        World world = new World("Tatooine", 0);

        worldRepository.save(world);
        worldRepository.touchAllWorlds();

        world = worldRepository.findOne(world.getId());
        assertNotNull(world.getUpdated());

    }

    @Test
    public void shouldCallExecuteWhenVoidReturnTypeOnQuery() {

        World tatooine = new World("Tatooine", 0);
        World dagobah = new World("Dagobah", 0);

        tatooine.addRocketRouteTo(dagobah);

        worldRepository.save(tatooine);
        worldRepository.touchOnlyWorldsReachableByRocket();

        tatooine = worldRepository.findOne(tatooine.getId());

        assertNotNull(tatooine.getUpdated());
        assertEquals(1, tatooine.getReachableByRocket().size());

        for (World world : tatooine.getReachableByRocket()) {
            assertNotNull(world.getUpdated());
        }

    }

    @Test
    public void shouldReturnStatisticsIfRequested() {

        World tatooine = new World("Tatooine", 0);

        worldRepository.save(tatooine);

        QueryStatistics stats = worldRepository.touchAllWorldsWithStatistics();

        assertEquals(1, stats.getPropertiesSet());

    }

}
