package org.springframework.data.neo4j.template.context;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.configuration.HttpDriverConfiguration;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author vince
 */
@Configuration
@EnableTransactionManagement
public class Neo4jTemplateConfiguration extends HttpDriverConfiguration {

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.examples.movies.domain");
    }

    @Bean
    public Neo4jOperations template() throws Exception {
        return new Neo4jTemplate(getSession());
    }

}
