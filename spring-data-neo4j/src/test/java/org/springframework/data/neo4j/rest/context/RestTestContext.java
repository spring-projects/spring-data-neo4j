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

package org.springframework.data.neo4j.rest.context;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.neo4j.ogm.session.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.data.neo4j.server.RemoteServer;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Configuration
@Import(RepositoryRestMvcConfiguration.class)
@ComponentScan({"org.springframework.data.neo4j.rest"})
@EnableNeo4jRepositories("org.springframework.data.neo4j.rest.repo")
@EnableTransactionManagement
public class RestTestContext extends Neo4jConfiguration {

    /** The local TCP port on which the Neo4j server created by this test context will listen. */
    public static final int NEO4J_TEST_PORT = 7879;

    @Bean
    @Override
    public SessionFactory getSessionFactory() {
        return new SessionFactory("org.springframework.data.neo4j.rest.domain");
    }

    @Bean
    @Override
    public Neo4jServer neo4jServer() {
        return new RemoteServer("http://localhost:" + NEO4J_TEST_PORT);
    }

    @Bean
    public MappingContext<?, ?> springDataCommonsMappingContext() {
        return new Neo4jMappingContext(getSessionFactory().metaData());
    }

    @Bean
    public HandlerExceptionResolver loggingExceptionResolver() {
        // ensure exceptions caught in the dispatcher servlet get logged and not swallowed like they do by default
        return new LoggingExceptionResolver();
    }

    static class LoggingExceptionResolver implements HandlerExceptionResolver, Ordered {

        @Override
        public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
            ex.printStackTrace(System.err);
            return null; // allows propagation down handler chain
        }

        @Override
        public int getOrder() {
            return 1;
        }

    }

}
