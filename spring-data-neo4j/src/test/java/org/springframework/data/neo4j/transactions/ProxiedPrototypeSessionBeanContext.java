package org.springframework.data.neo4j.transactions;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.*;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author vince
 */
@Configuration
@ComponentScan(basePackages = "org.springframework.data.neo4j.transactions.service")
@EnableTransactionManagement
@EnableNeo4jRepositories
public class ProxiedPrototypeSessionBeanContext extends Neo4jConfiguration {

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.transactions.domain");
    }

    @Override
    @Bean
    @Scope( value = ConfigurableBeanFactory.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.INTERFACES )
    public Session getSession() throws Exception {
        return getSessionFactory().openSession();
    }

}
