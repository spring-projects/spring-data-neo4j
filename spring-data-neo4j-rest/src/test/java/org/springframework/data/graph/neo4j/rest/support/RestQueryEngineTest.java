package org.springframework.data.graph.neo4j.rest.support;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.core.GraphDatabase;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.graph.neo4j.support.GraphRepositoryTest;
import org.springframework.data.graph.neo4j.support.query.QueryEngineTest;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

/**
 * @author mh
 * @since 23.06.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml",
    "classpath:RestTest-context.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class RestQueryEngineTest extends QueryEngineTest {

    @Autowired
    RestGraphDatabase restGraphDatabase;

    @BeforeClass
    public static void startDb() throws Exception {
        RestTestBase.startDb();
    }

    @BeforeTransaction
    public void cleanDb() {
        RestTestBase.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        RestTestBase.shutdownDb();
    }

    @Override
    protected GraphDatabase createGraphDatabase() throws Exception {
        restGraphDatabase.setConversionService(conversionService);
        return restGraphDatabase;
    }
}