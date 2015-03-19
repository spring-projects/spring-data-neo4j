/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd"
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.testutil;

import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * {@link DatabaseIntegrationTest} that starts the {@link WrappingNeoServerBootstrapper} as well, in order to make the Neo4j
 * browser and potentially custom managed and unmanaged extensions available for testing.
 * <p>
 * This is generally useful for developers who use Neo4j in server mode and want to test their extensions, whilst being able to
 * access the {@link org.neo4j.graphdb.GraphDatabaseService} object using {@link #getDatabase()}.
 * </p>
 * By overriding {@link #neoServerPort()}, you can change the port number of which the server runs.
 */
@SuppressWarnings("deprecation")
public abstract class WrappingServerIntegrationTest extends DatabaseIntegrationTest {

    private static final int DEFAULT_NEO_PORT = 7575;

    private WrappingNeoServerBootstrapper bootstrapper;
    private int neoServerPort = -1;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        startServerWrapper();
    }

    private static int findOpenLocalPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            System.err.println("Unable to establish local port due to IOException: " + e.getMessage()
                    + "\nDefaulting instead to use: " + DEFAULT_NEO_PORT);
            e.printStackTrace(System.err);

            return DEFAULT_NEO_PORT;
        }
    }

    @Override
    public void tearDown() throws Exception {
        bootstrapper.stop();
        super.tearDown();
    }

    private void startServerWrapper() {
        ServerConfigurator configurator = new ServerConfigurator((GraphDatabaseAPI) getDatabase());
        populateConfigurator(configurator);
        bootstrapper = new WrappingNeoServerBootstrapper((GraphDatabaseAPI) getDatabase(), configurator);
        bootstrapper.start();
    }

    /**
     * Populate server configurator with additional configuration. This method should rarely be overridden. In order to register
     * extensions, provide additional server config (including changing the port on which the server runs), please override one
     * of the methods below.
     *
     * @param configurator to populate.
     */
    protected void populateConfigurator(ServerConfigurator configurator) {
        configurator.configuration().addProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY, neoServerPort());
    }

    /**
     * Provide the port number on which the server will run.
     *
     * @return The local TCP port number of the Neo4j sever
     */
    protected int neoServerPort() {
        if (neoServerPort < 0) {
            neoServerPort = findOpenLocalPort();
        }
        return neoServerPort;
    }

    /**
     * Provide the base URL against which to execute tests.
     *
     * @return base URL.
     */
    protected String baseNeoUrl() {
        return "http://localhost:" + neoServerPort();
    }

}
