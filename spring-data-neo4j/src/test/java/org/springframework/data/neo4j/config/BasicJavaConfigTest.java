package org.springframework.data.neo4j.config;

import org.junit.After;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.neo4j.config.spring.BasicJavaConfig;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.repositories.PersonRepository;

import static org.junit.Assert.assertNotNull;

/**
 * Create SDN using (non test based) Java Config - No XML in site!
 */
public class BasicJavaConfigTest {

    AnnotationConfigApplicationContext appCtx;

    @After
    public void tearDown() {
        if (appCtx != null) appCtx.stop();
    }

    @Test
    public void verifyAppCtxStartsCorrectlyWithJavaConfig() {
        startJavaBasedAppCtx();
    }

    private void startJavaBasedAppCtx() {
        appCtx = new AnnotationConfigApplicationContext();
        appCtx.register(BasicJavaConfig.class);
        appCtx.registerShutdownHook();
        appCtx.refresh();
    }

    @Test
    public void testBasicRepositoryFunctionality() {
        startJavaBasedAppCtx();
        GraphDatabaseService graphDatabaseService = appCtx.getBean(GraphDatabaseService.class);
        PersonRepository personRepository = appCtx.getBean(PersonRepository.class);
        try (Transaction tx = graphDatabaseService.beginTx()) {
            Person person = new Person("Howdy",50);
            personRepository.save(person);
            assertNotNull(person.getId());
            tx.success();
        }
    }

}
