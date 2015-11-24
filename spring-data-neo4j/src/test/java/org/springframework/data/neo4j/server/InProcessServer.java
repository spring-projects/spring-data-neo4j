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

package org.springframework.data.neo4j.server;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.testutil.TestServer;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
public class InProcessServer implements Neo4jServer {

    private TestServer testServer;

    public InProcessServer(Driver driver)  {
        this.testServer = new TestServer(driver);
    }

    public String url() {
        return (String) this.testServer.driver().getConfig("server");
    }

    @Override
    public String username() {
        return (String) testServer.driver().getConfig("username");
    }

    @Override
    public String password() {
        return (String) this.testServer.driver().getConfig("password");
    }

    public GraphDatabaseService database() {
        return testServer.getGraphDatabaseService();
    }
}