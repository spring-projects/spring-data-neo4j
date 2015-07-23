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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.neo4j.graphdb.GraphDatabaseService;

/**
 * JUnit {@link TestRule} that runs a {@link GraphDatabaseService} behind a Spring Data REST API to provide an infrastructure
 * for testing integration of SDN with Spring Data REST.
 *
 * @author Adam George
 */
public class RestIntegrationTestRule implements TestRule {

    private static final int REST_SERVER_PORT = 9090;

    private final RestTestServer restServer;

    /**
     * Constructs a new {@link RestIntegrationTestRule} that initialises a test {@link RestTestServer} listening on TCP port 9090.
     */
    public RestIntegrationTestRule() {
        this.restServer = new RestTestServer(REST_SERVER_PORT);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        this.restServer.start();

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    restServer.stop();
                }
            }
        };
    }

    public GraphDatabaseService getGraphDatabaseService() {
        return this.restServer.getNeo4jDatabase().getGraphDatabaseService();
    }

    /**
     * @return The URL of the REST-enabled HTTP server (without a trailing slash)
     */
    public String getRestBaseUrl() {
        return "http://localhost:" + REST_SERVER_PORT;
    }

}
