/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.rest.support;


import java.net.URISyntaxException;

import org.neo4j.server.NeoServer;
import org.neo4j.server.WrappingNeoServerBootstrapper;
import org.neo4j.server.configuration.Configurator;
import org.neo4j.server.configuration.ServerConfigurator;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;

public class RestTestHelper
{

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 7470;
    private static NeoServer neoServer;
    private static final String SERVER_ROOT_URI = "http://" + HOSTNAME + ":" + PORT + "/db/data/";
    private static ImpermanentGraphDatabase db;

    public void startServer() throws Exception {
        db = new ImpermanentGraphDatabase();
        final ServerConfigurator configurator = new ServerConfigurator(db);
        configurator.configuration().setProperty(Configurator.WEBSERVER_PORT_PROPERTY_KEY,PORT);
        final WrappingNeoServerBootstrapper bootstrapper = new WrappingNeoServerBootstrapper(db, configurator);
        bootstrapper.start();
        neoServer = bootstrapper.getServer();
    }

    public GraphDatabase createGraphDatabase() throws URISyntaxException {
        return new SpringRestGraphDatabase(SERVER_ROOT_URI);
    }

    public void cleanDb() {
        db.cleanContent();
    }

    public static void shutdownServer() {
        neoServer.stop();
        neoServer = null;
    }
}
