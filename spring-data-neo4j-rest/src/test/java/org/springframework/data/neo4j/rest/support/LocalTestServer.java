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

import org.mortbay.component.LifeCycle;
import org.mortbay.jetty.Server;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.configuration.PropertyFileConfigurator;
import org.neo4j.server.database.Database;
import org.neo4j.server.database.WrappingDatabase;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.modules.ServerModule;
import org.neo4j.server.modules.ThirdPartyJAXRSModule;
import org.neo4j.server.startup.healthcheck.StartupHealthCheck;
import org.neo4j.server.web.Jetty6WebServer;
import org.neo4j.server.web.WebServer;
import org.neo4j.test.ImpermanentGraphDatabase;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

public class LocalTestServer {
    private CommunityNeoServer neoServer;
    private final int port;
    private final String hostname;
    protected String propertiesFile = "test-db.properties";
    private final ImpermanentGraphDatabase graphDatabase;

    public LocalTestServer() {
        this("localhost",7473);
    }

    public LocalTestServer(String hostname, int port) {
        this.port = port;
        this.hostname = hostname;
        graphDatabase = new ImpermanentGraphDatabase();
    }

    public void start() {
        if (neoServer!=null) throw new IllegalStateException("Server already running");
        URL url = getClass().getResource("/" + propertiesFile);
        if (url==null) throw new IllegalArgumentException("Could not resolve properties file "+propertiesFile);
        final Jetty6WebServer jettyWebServer = new Jetty6WebServer() {
            @Override
            protected void startJetty() {
                final Server jettyServer = getJetty();
                jettyServer.setStopAtShutdown(true);
                final JettyStartupListener startupListener = new JettyStartupListener();
                jettyServer.getServer().addLifeCycleListener(startupListener);
                // System.err.println("jetty is started before notification " + jettyServer.isStarted());

                super.startJetty();

                startupListener.await();
                jettyServer.removeLifeCycleListener(startupListener);
                // System.err.println("jetty is started after notification " + jettyServer.isStarted());
            }

            @Override
            public void stop() {
                final Server jettyServer = getJetty();
                final JettyStartupListener listener = new JettyStartupListener();
                jettyServer.getServer().addLifeCycleListener(listener);

                super.stop();

                listener.await();
                jettyServer.removeLifeCycleListener(listener);
            }
        };
        neoServer = new CommunityNeoServer(new PropertyFileConfigurator(new File(url.getPath()))) {
            @Override
            protected int getWebServerPort() {
                return port;
            }

            @Override
            protected StartupHealthCheck createHealthCheck() {
                return new StartupHealthCheck();
            }

            @Override
            protected Database createDatabase() {
                return new WrappingDatabase(graphDatabase);
            }


            @Override
            protected WebServer createWebServer() {
                return jettyWebServer;
            }

            @Override
            protected Iterable<ServerModule> createServerModules() {
                return asList(new RESTApiModule(webServer,database,configurator.configuration()),new ThirdPartyJAXRSModule(webServer,configurator));
            }
        };
        neoServer.start();
    }

    public void stop() {
        try {
        neoServer.stop();
        } catch(Exception e) {
            System.err.println("Error stopping server: "+e.getMessage());
        }
        neoServer=null;
    }

    public int getPort() {
        return port;
    }

    public String getHostname() {
        return hostname;
    }

    public LocalTestServer withPropertiesFile(String propertiesFile) {
        this.propertiesFile = propertiesFile;
        return this;
    }
    public Database getDatabase() {
        return neoServer.getDatabase();
    }

    public URI baseUri() {
        return neoServer.baseUri();
    }

    public void cleanDb() {
        Neo4jDatabaseCleaner cleaner = new Neo4jDatabaseCleaner(getGraphDatabase());
        cleaner.cleanDb();
    }

    public GraphDatabaseService getGraphDatabase() {
        return getDatabase().graph;
    }

    private static class JettyStartupListener implements LifeCycle.Listener {
        CountDownLatch latch=new CountDownLatch(1);
        public void await() {
            try {
                latch.await(10, TimeUnit.SECONDS);
            } catch(InterruptedException ie) {
                System.err.println("ERROR startup took too long - await()");
                throw new RuntimeException("Jetty did not start correctly",ie);
            }
        }

        @Override
        public void lifeCycleStarting(LifeCycle event) {
            System.err.println("STARTING");
        }

        @Override
        public void lifeCycleStarted(LifeCycle event) {
            System.err.println("STARTED");
            latch.countDown();
        }

        @Override
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {
            System.out.println("FAILURE "+cause.getMessage());
            latch.countDown();
            throw new RuntimeException(cause);
        }

        @Override
        public void lifeCycleStopping(LifeCycle event) {
            System.err.println("STOPPING");
        }

        @Override
        public void lifeCycleStopped(LifeCycle event) {
            System.err.println("STOPPED");
            latch.countDown();
        }
    }
}
