package org.springframework.data.neo4j.examples.hellograph;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.hellograph.domain.World;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

@ContextConfiguration(locations = "classpath:/spring/helloWorldContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class GalaxyServiceTest {
	
	@Autowired
	private GalaxyService galaxyService;
	
	@Autowired
	private Neo4jTemplate template;
	
	@Rollback(false)
	@BeforeTransaction
	public void cleanUpGraph() {
		Neo4jHelper.cleanDb(template);
	}
	
	@Test
    public void shouldAllowDirectWorldCreation() {
		assertEquals(0, galaxyService.getNumberOfWorlds());
		World myWorld = galaxyService.createWorld("mine", 0);
        assertEquals(1, galaxyService.getNumberOfWorlds());
        
        Iterable<World> foundWorlds = galaxyService.getAllWorlds();
        World mine = foundWorlds.iterator().next();
        assertEquals(myWorld.getName(), mine.getName());
    }
	
	@Test
    public void shouldHaveCorrectNumberOfWorlds() {
		galaxyService.makeSomeWorlds();
        assertEquals(13, galaxyService.getNumberOfWorlds());
    }

    @Test
    public void shouldFindWorldsById() {
    	galaxyService.makeSomeWorlds();
    	
        for(World world : galaxyService.getAllWorlds()) {
        	World foundWorld = galaxyService.findWorldById(world.getId()); 
            assertNotNull(foundWorld);
        }
    }
    
    @Test
    public void shouldFindWorldsByName() {
    	galaxyService.makeSomeWorlds();
    	
        for(World world : galaxyService.getAllWorlds()) {
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
        for(World foundWorld : foundWorlds) {
            assertTrue(madeWorlds.contains(foundWorld));
            countOfFoundWorlds++;
        }

        assertEquals(madeWorlds.size(), countOfFoundWorlds);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void shouldFindWorldsWith1Moon() {
        galaxyService.makeSomeWorlds();
        
        for(World worldWithOneMoon : galaxyService.findAllByNumberOfMoons(1)) {
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
