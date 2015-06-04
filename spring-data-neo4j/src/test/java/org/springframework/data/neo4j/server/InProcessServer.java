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

import org.neo4j.ogm.testutil.TestServer;

/**
 * @author Michal Bachman
 */
public class InProcessServer implements Neo4jServer {

    private TestServer testServer;

    public InProcessServer()  {
        this.testServer = new TestServer();
    }

    public String url() {
        return testServer.url();
    }

}