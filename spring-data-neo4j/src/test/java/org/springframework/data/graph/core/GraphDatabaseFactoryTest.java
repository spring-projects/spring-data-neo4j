package org.springframework.data.graph.core;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author mh
 * @since 29.03.11
 */
public class GraphDatabaseFactoryTest {

    @Test
    public void shouldCreateLocalDatabaseFromContext() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("GraphDatabaseFactory-context.xml");
        try {
            GraphDatabase graphDatabase = ctx.getBean("graphDatabase", GraphDatabase.class);
            assertThat(graphDatabase, is(not(nullValue())));
            assertThat(graphDatabase, is(instanceOf(DelegatingGraphDatabase.class)));
        } finally {
            ctx.close();
        }

    }
    @Test
    public void shouldCreateLocalDatabase() throws Exception {
        GraphDatabaseFactory factory = new GraphDatabaseFactory();
        try {
            factory.setStoreLocation("target/test-db");
            GraphDatabase graphDatabase = factory.getObject();
            assertThat(graphDatabase, is(not(nullValue())));
            assertThat(graphDatabase,is(instanceOf(DelegatingGraphDatabase.class)));
        } finally {
            factory.shutdown();
        }
    }
}
