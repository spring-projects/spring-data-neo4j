package org.springframework.data.graph.neo4j.config;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author mh
 * @since 23.03.11
 */
public class ConfigurationConfirmationTest {
    @Test(expected = BeanCreationException.class)
    public void testInvalidTransactionManagerFails() {
        ClassPathXmlApplicationContext appCtx = new ClassPathXmlApplicationContext("classpath:org/springframework/data/graph/neo4j/config/ConfigurationCofirmationTest-context.xml");
    }
}
