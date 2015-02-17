package org.springframework.data.neo4j.config;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.annotation.Resource;

@Configuration
public abstract class Neo4jConfiguration {

    private final Logger logger = LoggerFactory.getLogger(Neo4jConfiguration.class);

    @Resource
    private Environment environment;

    @Bean
    public Session getSession() throws Exception {
        logger.info("Initialising Neo4jSession");
        return getSessionFactory().openSession(neo4jServer().url());
    }

    @Bean
    public PersistenceExceptionTranslator persistenceExceptionTranslator() {
        return new PersistenceExceptionTranslator() {
            @Override
            public DataAccessException translateExceptionIfPossible(RuntimeException e) {
                throw e;
            }
        };
    }

    @Bean
    public PlatformTransactionManager transactionManager() throws Exception {
        logger.info("Initialising Neo4jTransactionManager");
        return new Neo4jTransactionManager(getSession());
    }

    @Bean
    public abstract Neo4jServer neo4jServer();

    @Bean
    public abstract SessionFactory getSessionFactory();


}
