/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.rest;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.neo4j.ogm.testutil.TestServer;
import org.springframework.data.neo4j.rest.context.RestTestContext;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Simple server that encapsulates a Neo4j database fronted by an HTTP server exposing repositories over Spring Data REST.
 */
public class RestTestServer {

    private TestServer neo4jDatabase;
    private Server restServer;
    private final int restServerPort;

    /**
     * Constructs a new {@link RestTestServer} on localhost:9090.
     */
    public RestTestServer() {
        this(9090);
    }

    /**
     * Constructs a new {@link RestTestServer} listening on the specified local TCP port.
     *
     * @param restServerPort The TCP port on which the RESTful HTTP server should listen
     */
    public RestTestServer(int restServerPort) {
        this.restServerPort = restServerPort;
    }

    /**
     * Starts the Spring Data REST server on the specified local port in front of a Neo4j database on localhost:7879, as
     * specified by the {@link RestTestContext} used to configure this server.
     */
    public void start() {
        this.neo4jDatabase = new TestServer(RestTestContext.NEO4J_TEST_PORT);
        this.restServer = new Server(this.restServerPort);

        AnnotationConfigWebApplicationContext wac = new AnnotationConfigWebApplicationContext();
        wac.register(RestTestContext.class);
        wac.refresh();

        ServletHolder dispatcherServletHolder = new ServletHolder();
        dispatcherServletHolder.setServlet(new DispatcherServlet(wac));

        ServletContextHandler handler = new ServletContextHandler();
        handler.addServlet(dispatcherServletHolder, "/*");
        handler.setContextPath("/");
        handler.setServer(this.restServer);
        handler.addEventListener(new ContextLoaderListener(wac));
        this.restServer.setHandler(handler);

        try {
            this.restServer.start();
        } catch (Exception e) {
            stop();
        }
    }

    /**
     * Shuts down both the database and the web server.
     */
    public void stop() {
        this.neo4jDatabase.shutdown();
        try {
            this.restServer.stop();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public TestServer getNeo4jDatabase() {
        return neo4jDatabase;
    }

}
