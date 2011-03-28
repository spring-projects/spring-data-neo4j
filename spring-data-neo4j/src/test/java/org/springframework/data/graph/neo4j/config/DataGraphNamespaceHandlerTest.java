package org.springframework.data.graph.neo4j.config;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author mh
 * @since 31.01.11
 */

public class DataGraphNamespaceHandlerTest {

    static class Config {
        @Autowired
        GraphDatabaseService graphDatabaseService;
        @Autowired
        DirectGraphRepositoryFactory graphRepositoryFactory;
        @Autowired
        GraphDatabaseContext graphDatabaseContext;
        @Autowired
        PlatformTransactionManager transactionManager;
    }

    @Test
    public void injectionForJustStoreDir() {
        assertInjected("");
    }
    @Test
    public void injectionForExistingGraphDatabaseService() {
        assertInjected("-external-embedded");
    }

    @Test
    public void injectionForCodeConfiguredExistingGraphDatabaseService() {
        assertInjected("-code");
    }

    @Test
    public void injectionForCrossStore() {
        assertInjected("-cross-store");
    }

    private void assertInjected(String testCase) {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:org/springframework/data/graph/neo4j/config/DataGraphNamespaceHandlerTest" + testCase + "-context.xml");
        Config config = ctx.getBean("config", Config.class);
        GraphDatabaseContext graphDatabaseContext = config.graphDatabaseContext;
        Assert.assertNotNull("graphDatabaseContext", graphDatabaseContext);
        EmbeddedGraphDatabase graphDatabaseService = (EmbeddedGraphDatabase) graphDatabaseContext.getGraphDatabaseService();
        Assert.assertEquals("store-dir", "target/config-test", graphDatabaseService.getStoreDir());
        Assert.assertNotNull("graphRepositoryFactory",config.graphRepositoryFactory);
        Assert.assertNotNull("graphDatabaseService",config.graphDatabaseService);
        Assert.assertNotNull("transactionManager",config.transactionManager);
        config.graphDatabaseService.shutdown();
    }

}
