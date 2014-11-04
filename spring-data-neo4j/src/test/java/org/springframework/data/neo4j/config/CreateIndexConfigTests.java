package org.springframework.data.neo4j.config;

import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.data.neo4j.support.mapping.DefaultEntityIndexCreator;
import org.springframework.data.neo4j.support.mapping.EntityIndexCreator;
import org.springframework.data.neo4j.support.mapping.NoEntityIndexCreator;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 19.10.14
 */
public class CreateIndexConfigTests {

    private ApplicationContext ctx;

    static class DefaultTestConfiguration extends Neo4jConfiguration {
        @Bean
        public GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    static class NoIndexTestConfiguration extends Neo4jConfiguration {
        NoIndexTestConfiguration() {
            setCreateIndex(false);
        }

        @Bean
        public GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }
    @Test
    public void testEnableIndexWithNoConfig() throws Exception {
        ctx = new AnnotationConfigApplicationContext(DefaultTestConfiguration.class);
        EntityIndexCreator entityIndexCreator = ctx.getBean("entityIndexCreator", EntityIndexCreator.class);
        assertEquals(DefaultEntityIndexCreator.class,entityIndexCreator.getClass());
    }

    @Test
    public void testDisableIndexWithConstructorParam() throws Exception {
        ctx = new AnnotationConfigApplicationContext(NoIndexTestConfiguration.class);
        EntityIndexCreator entityIndexCreator = ctx.getBean("entityIndexCreator", EntityIndexCreator.class);
        assertEquals(NoEntityIndexCreator.class,entityIndexCreator.getClass());
    }
    @Test
    public void testDisableIndexWithXmlConfig() throws Exception {
        ctx = new ClassPathXmlApplicationContext("CreateIndexConfigTests-NoIndex-context.xml",getClass());
        EntityIndexCreator entityIndexCreator = ctx.getBean("entityIndexCreator", EntityIndexCreator.class);
        assertEquals(NoEntityIndexCreator.class,entityIndexCreator.getClass());
    }
    @Test
    public void testEnableIndexWithDefaultXmlConfig() throws Exception {
        ctx = new ClassPathXmlApplicationContext("CreateIndexConfigTests-DefaultIndex-context.xml",getClass());
        EntityIndexCreator entityIndexCreator = ctx.getBean("entityIndexCreator", EntityIndexCreator.class);
        assertEquals(DefaultEntityIndexCreator.class,entityIndexCreator.getClass());
    }
    @Test
    public void testEnableIndexWithXmlConfig() throws Exception {
        ctx = new ClassPathXmlApplicationContext("CreateIndexConfigTests-Index-context.xml",getClass());
        EntityIndexCreator entityIndexCreator = ctx.getBean("entityIndexCreator", EntityIndexCreator.class);
        assertEquals(DefaultEntityIndexCreator.class,entityIndexCreator.getClass());
    }

    @After
    public void tearDown() throws Exception {
        if (ctx != null) ctx.close();

    }
}
