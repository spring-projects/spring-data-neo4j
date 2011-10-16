package org.springframework.data.neo4j.examples.hellograph;

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

import java.util.Map;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 17.02.11
 * Added to check for some aspectj-snapshot build errors.
 */
@ContextConfiguration(locations = "classpath:spring/helloWorldContext.xml")
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class WorldCounterTest {

    @Autowired
    private Neo4jTemplate template;

    @Rollback(false)
    @BeforeTransaction
    public void clearDatabase()
    {
        Neo4jHelper.cleanDb(template);
    }

    @Test
    public void testCountMoons() throws Exception {
        WorldCounter counter = new WorldCounter();
        Map<String,Integer> result = counter.countMoons(asList(new World("earth", 1)));
        assertEquals("earth has one moon",(Integer)1,result.get("earth"));
    }
}
