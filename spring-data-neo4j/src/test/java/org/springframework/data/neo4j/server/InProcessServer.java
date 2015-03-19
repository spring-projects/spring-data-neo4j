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

import org.neo4j.server.NeoServer;
import org.neo4j.server.helpers.CommunityServerBuilder;

import java.io.IOException;

/**
 * @author Michal Bachman
 */
public class InProcessServer implements Neo4jServer {

    private final NeoServer neoServer;
    protected int neoPort;

    public InProcessServer()  {
        neoPort = TestUtils.getAvailablePort();
        try {
            neoServer = CommunityServerBuilder.server().onPort(neoPort).build();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    neoServer.stop();
                }
            });
            neoServer.start();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public String url() {
        return neoServer.baseUri().toString();
    }


}