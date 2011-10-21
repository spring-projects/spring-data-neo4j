package org.springframework.data.neo4j.examples.hellograph;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

import static junit.framework.Assert.assertEquals;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.StringContains.containsString;

/**
 * Exploratory testing of Spring Data Neo4j using
 * the WorldRepositoryImpl.
 */
@ContextConfiguration(locations = "classpath:spring/helloWorldContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class WorldRepositoryTest
{

    @Autowired
	private WorldRepository galaxy;

	@Autowired
	private Neo4jTemplate template;

	@Rollback(false)
    @BeforeTransaction
    public void clearDatabase()
    {
		Neo4jHelper.cleanDb(template);
    }

    @Test
    public void shouldAllowDirectWorldCreation()
    {
        assertEquals(0, (long) template.count(World.class));
        World myWorld = galaxy.save(new World( "mine", 0 ));
        assertEquals(1, (long) template.count(World.class));
        World foundWorld = galaxy.findOne(myWorld.id);
        assertEquals(myWorld.getName(), foundWorld.getName());
    }

    @Test
    public void shouldPopulateGalaxyWithWorlds()
    {
        Iterable<World> worlds = galaxy.makeSomeWorlds();
        assertNotNull( worlds );
    }


    @Test
    public void shouldHaveCorrectNumberOfWorlds()
    {
        galaxy.makeSomeWorlds();
        assertEquals(13, (long) galaxy.count());
    }

    @Test
    public void shouldFindAllWorlds()
    {
        Collection<World> madeWorlds = galaxy.makeSomeWorlds();
        Iterable<World> foundWorlds = galaxy.findAll();

        int countOfFoundWorlds = 0;
        for ( World foundWorld : foundWorlds )
        {
            assertTrue( madeWorlds.contains( foundWorld ) );
            countOfFoundWorlds++;
        }

        assertEquals( madeWorlds.size(), countOfFoundWorlds );
    }

    @Test
    public void shouldFindWorldsByName()
    {
        for ( World w : galaxy.makeSomeWorlds() )
        {
            assertNotNull( galaxy.findWorldNamed( w.getName() ) );
        }
    }

	@SuppressWarnings("unchecked")
    @Test
    public void shouldFindWorldsWith1Moon()
    {
        galaxy.makeSomeWorlds();
        for ( World worldWithOneMoon : galaxy.findWorldsWithMoons( 1 ) )
        {
        	assertThat( worldWithOneMoon.getName(), is( anyOf( containsString( "Earth" ), containsString( "Midgard" ) ) ) );
        }
    }

    @Test
    public void shouldReachMarsFromEarth()
    {
        galaxy.makeSomeWorlds();

        World earth = galaxy.findWorldNamed( "Earth" );
        World mars = galaxy.findWorldNamed( "Mars" );

        assertTrue( mars.canBeReachedFrom( earth ) );
        assertTrue( earth.canBeReachedFrom( mars ) );
    }

}
