package org.springframework.data.neo4j.configuration;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.server.InProcessServer;
import org.springframework.data.neo4j.server.Neo4jServer;

/**
 * @author vince
 */
public abstract class HttpDriverConfiguration extends Neo4jConfiguration {

    @Bean
    public Neo4jServer neo4jServer() {
        return new InProcessServer(driver());
    }

    @Bean
    public GraphDatabaseService graphDatabaseService() {
        return ((InProcessServer) neo4jServer()).database();
    }
}
