package org.springframework.data.neo4j.examples.hellograph;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(locations = "classpath:/spring/helloWorldContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class GalaxyServiceTest {
	
	@Autowired
	private GalaxyService galaxyService;
	
	@Autowired
	private Neo4jTemplate template;
	
	@Before
	public void cleanUpGraph() {
		Neo4jHelper.cleanDb(template);
	}
	
	@Test
	public void galaxyShouldBeEmptyUntilExplicitlyPopulated() {
		assertTrue(galaxyService.findAllWorlds().isEmpty());
	}
	
	@Test
	public void populatedGalaxyShouldHaveEarth() {
		galaxyService.makeSureGalaxyIsNotEmpty();
		WorldDto earth = galaxyService.findWorldNamed("Earth");
		assertNotNull(earth);
		assertEquals("Earth", earth.getName());
	}
	
	@Test
	public void earthShouldHaveOnlyOneReachableWorld() {
		galaxyService.makeSureGalaxyIsNotEmpty();
		WorldDto earth = galaxyService.findWorldNamed("Earth");
		List<WorldDto> reachableWorlds = galaxyService.findReachableWorlds(earth.getId());
		assertEquals(1, reachableWorlds.size());
	}
	
	@Test
	public void populatedGalaxyShouldNotHaveKrypton() {
		galaxyService.makeSureGalaxyIsNotEmpty();
		WorldDto planetPopsicle = galaxyService.findWorldNamed("Krypton");
		assertNull(planetPopsicle);
	}
}
