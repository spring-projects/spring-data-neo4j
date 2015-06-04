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
package org.springframework.data.neo4j.extensions;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 *
 * Note how the repository base class for all our repositories is overridden
 * using the 'repositoryBaseClass' attribute.
 * This annotation change allows all our repositories to easily extend one or more
 * additional interfaces.
 *
 * @author: Vince Bickers
 */
@Configuration
@EnableNeo4jRepositories(repositoryBaseClass = CustomGraphRepositoryImpl.class)
@EnableTransactionManagement
public class CustomPersistenceContext extends Neo4jConfiguration {

    @Override
    @Bean
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.extensions.domain");
    }

    @Bean
    @Override
    public Neo4jServer neo4jServer() {
        return new RemoteServer("http://localhost:7879");
    }

}

