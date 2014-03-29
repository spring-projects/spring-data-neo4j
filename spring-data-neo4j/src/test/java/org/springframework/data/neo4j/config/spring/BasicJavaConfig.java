package org.springframework.data.neo4j.config.spring;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;

@Configuration
@EnableNeo4jRepositories(basePackages = "org.springframework.data.neo4j.repositories")
public class BasicJavaConfig extends Neo4jConfiguration {

    public BasicJavaConfig() {
        setBasePackage("org.springframework.data.neo4j.model");
    }

    @Bean
    public GraphDatabaseService graphDatabaseService() {
        return new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

}
