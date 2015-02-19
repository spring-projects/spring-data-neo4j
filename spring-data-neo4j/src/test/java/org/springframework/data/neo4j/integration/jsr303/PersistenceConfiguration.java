package org.springframework.data.neo4j.integration.jsr303;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.*;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.InProcessServer;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan
@EnableNeo4jRepositories("org.springframework.data.neo4j.integration.jsr303.repo")
@EnableTransactionManagement
public class PersistenceConfiguration extends Neo4jConfiguration {

    @Bean
    @Override
    public Neo4jServer neo4jServer() {
        return new InProcessServer();
    }

    @Bean
    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.integration.jsr303.domain");
    }

    @Override
    @Bean
    @Scope(value = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public Session getSession() throws Exception {
        return super.getSession();
    }

}
