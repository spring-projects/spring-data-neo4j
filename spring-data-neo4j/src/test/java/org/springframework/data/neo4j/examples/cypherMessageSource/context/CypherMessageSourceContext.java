/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.examples.cypherMessageSource.context;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.context.support.CypherMessageSource;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Eric Spiegelberg - eric [at] miletwentyfour [dot] com
 */
@Configuration
@EnableTransactionManagement
@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.cypherMessageSource.repo")
@ComponentScan({ "org.springframework.data.neo4j.examples.cypherMessageSource" })
public class CypherMessageSourceContext extends Neo4jConfiguration {

    @Bean
    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.examples.cypherMessageSource.domain");
    }

    @Bean
    public MessageSource getMessageSource() {

        CypherMessageSource cypherMessageSouce = new CypherMessageSource();

        return cypherMessageSouce;
    }

}