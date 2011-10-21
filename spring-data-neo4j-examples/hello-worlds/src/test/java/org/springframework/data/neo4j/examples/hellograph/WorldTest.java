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

import static org.junit.Assert.assertNull;

/**
 * Exploratory unit-tests for the Spring Data Neo4j annotated World entity.
 * 
 * Since the World is a @NodeEntity, the SpringDataGraph must
 * be setup before you can even create instances of the POJO.
 */
@ContextConfiguration(locations = "classpath:spring/helloWorldContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@Ignore("TODO ABK")
public class WorldTest
{

	@Autowired
	private Neo4jTemplate template;

	@Rollback(false)
    @BeforeTransaction
    public void clearDatabase()
    {
		Neo4jHelper.cleanDb(template);
    }

    @Test
    public void shouldBeSimpleToCreateNewEntities()
    {
        @SuppressWarnings("unused")
		World w = new World();
    }

    @Test
    public void shouldHaveNullNameUsingDefaultConstructor()
    {
        World w = new World();
        assertNull(w.getName());
    }
}
