/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
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
package org.neo4j.rest.graphdb;

import org.eclipse.jetty.util.component.LifeCycle;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.GraphDatabaseAPI;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.logging.Logging;
import org.neo4j.server.CommunityNeoServer;
import org.neo4j.server.configuration.PropertyFileConfigurator;
import org.neo4j.server.database.Database;
import org.neo4j.server.database.WrappedDatabase;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.modules.ServerModule;
import org.neo4j.server.modules.ThirdPartyJAXRSModule;
import org.neo4j.server.preflight.PreFlightTasks;
import org.neo4j.server.web.Jetty9WebServer;
import org.neo4j.server.web.WebServer;
import org.neo4j.test.TestGraphDatabaseFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 24.03.11
 */
public class LocalTestServer {
    private CommunityNeoServer neoServer;
    private final int port;
    private final String hostname;
    protected String propertiesFile = "test-db.properties";
    private final GraphDatabaseAPI graphDatabase;
    private String userAgent;

    public LocalTestServer() {
        this("localhost",7473);
    }

    public LocalTestServer(String hostname, int port) {
        this.port = port;
        this.hostname = hostname;
        graphDatabase = (GraphDatabaseAPI) new TestGraphDatabaseFactory().newImpermanentDatabase();
    }

    public void start() {
        if (neoServer!=null) throw new IllegalStateException("Server already running");
        URL url = getClass().getResource("/" + propertiesFile);
        if (url==null) throw new IllegalArgumentException("Could not resolve properties file "+propertiesFile);
        Logging logging = graphDatabase.getDependencyResolver().resolveDependency(Logging.class);
        final Jetty9WebServer jettyWebServer = new Jetty9WebServer(logging); /* {
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
        }; */
        jettyWebServer.addFilter(new Filter() {
            public void init(FilterConfig filterConfig) throws ServletException { }

            public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
                userAgent = ((HttpServletRequest)request).getHeader("User-Agent");
                filterChain.doFilter(request, response);
            }

            public void destroy() { }
        },"/*");
        neoServer = new CommunityNeoServer(new PropertyFileConfigurator(new File(url.getPath())), new Database.Factory() {
            @Override
            public Database newDatabase(Config config, Logging logging) {
                return new WrappedDatabase(graphDatabase);
            }
        },logging) {
            @Override
            protected int getWebServerPort() {
                return port;
            }

            @Override
            protected PreFlightTasks createPreflightTasks() {
                return new PreFlightTasks(logging);
            }

            @Override
            protected WebServer createWebServer() {
                return jettyWebServer;
            }

            @Override
            protected Iterable<ServerModule> createServerModules() {
                return asList(new RESTApiModule(webServer,database,configurator.configuration(),logging),new ThirdPartyJAXRSModule(webServer,configurator,logging,this));
            }
        };
        neoServer.start();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        return getDatabase().getGraph();
    }

    public String getUserAgent() {
        return userAgent;
    }

    private static class JettyStartupListener implements LifeCycle.Listener {
        CountDownLatch latch=new CountDownLatch(1);
        public void await() {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(ie);
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
            System.err.println("FAILURE "+cause.getMessage());
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
