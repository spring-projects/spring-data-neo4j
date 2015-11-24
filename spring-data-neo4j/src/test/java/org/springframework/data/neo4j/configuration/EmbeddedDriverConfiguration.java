package org.springframework.data.neo4j.configuration;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.config.Neo4jConfiguration;

/**
 * @author vince
 */
public abstract class EmbeddedDriverConfiguration extends Neo4jConfiguration {

    @Override
    @Bean
    public Driver driver() {
        return new EmbeddedDriver(graphDatabaseService());
    }

    @Bean
    public GraphDatabaseService graphDatabaseService() {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }
}
