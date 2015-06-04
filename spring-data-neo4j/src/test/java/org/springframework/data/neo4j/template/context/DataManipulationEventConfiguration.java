/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.template.context;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.event.AfterDeleteEvent;
import org.springframework.data.neo4j.event.AfterSaveEvent;
import org.springframework.data.neo4j.event.BeforeDeleteEvent;
import org.springframework.data.neo4j.event.BeforeSaveEvent;
import org.springframework.data.neo4j.template.TestNeo4jEventListener;
import org.springframework.data.neo4j.server.InProcessServer;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Configuration bean for testing data manipulation events supported by <code>Neo4jTemplate</code>.
 *
 * @author Adam George
 */
@Configuration
@EnableTransactionManagement
public class DataManipulationEventConfiguration extends Neo4jConfiguration {

    @Override
    public Neo4jServer neo4jServer() {
        return new InProcessServer();
    }

    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.examples.movies.domain");
    }

    @Bean
    public Neo4jOperations getNeo4jTemplate() throws Exception {
        return new Neo4jTemplate(getSession());
    }

    @Bean
    public ApplicationListener<BeforeSaveEvent> beforeSaveEventListener() {
        return new TestNeo4jEventListener<BeforeSaveEvent>() {};
    }

    @Bean
    public ApplicationListener<AfterSaveEvent> afterSaveEventListener() {
        return new TestNeo4jEventListener<AfterSaveEvent>() {};
    }

    @Bean
    public ApplicationListener<BeforeDeleteEvent> beforeDeleteEventListener() {
        return new TestNeo4jEventListener<BeforeDeleteEvent>() {};
    }

    @Bean
    public ApplicationListener<AfterDeleteEvent> afterDeleteEventListener() {
        return new TestNeo4jEventListener<AfterDeleteEvent>() {};
    }

}
