package org.springframework.data.neo4j.integration.movies.context;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan({"org.springframework.data.neo4j.integration.movies"})
@EnableNeo4jRepositories("org.springframework.data.neo4j.integration.movies.repo")
@EnableTransactionManagement
public class PersistenceContext extends Neo4jConfiguration {

    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.integration.movies.domain");
    }

    @Bean
    public Neo4jServer neo4jServer() {
        return new RemoteServer("http://localhost:7879");
    }
}
