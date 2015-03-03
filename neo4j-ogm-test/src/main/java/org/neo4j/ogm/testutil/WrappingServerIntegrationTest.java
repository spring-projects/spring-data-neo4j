/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j-OGM-Test.
 *
 * Neo4j-OGM-Test is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
