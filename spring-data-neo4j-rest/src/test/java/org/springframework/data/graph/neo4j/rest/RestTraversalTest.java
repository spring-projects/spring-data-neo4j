package org.springframework.data.graph.neo4j.rest;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.neo4j.rest.graphdb.RestTestBase;
import org.springframework.data.graph.neo4j.support.TraversalTest;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * @author mh
 * @since 28.03.11
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
        "classpath:RestTest-context.xml"} )
@TestExecutionListeners( {CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class} )
public class RestTraversalTest extends TraversalTest
{

    @BeforeClass
    public static void startDb() throws Exception
    {
        RestTestBase.startDb();
    }

    @Before
    public void cleanDb()
    {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb()
    {
        RestTestBase.shutdownDb();

    }

}
