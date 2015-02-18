package org.springframework.data.neo4j.integration.helloworld.context;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.neo4j.server.InProcessServer;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan({"org.springframework.data.neo4j.integration.helloworld.*"})
@PropertySource("classpath:helloworld.properties")
@EnableNeo4jRepositories("org.springframework.data.neo4j.integration.helloworld.repo")
@EnableTransactionManagement
public class HelloWorldContext extends Neo4jConfiguration {

    @Bean
    @Override
    public Neo4jServer neo4jServer() {
        return new InProcessServer();
        //production: return new RemoteServer(environment.getRequiredProperty("url");
    }

    @Bean
    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.integration.helloworld.domain");
    }

}
