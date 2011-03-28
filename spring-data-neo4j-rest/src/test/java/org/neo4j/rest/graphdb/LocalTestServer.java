package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.server.AddressResolver;
import org.neo4j.server.NeoServerWithEmbeddedWebServer;
import org.neo4j.server.database.Database;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.modules.ThirdPartyJAXRSModule;
import org.neo4j.server.startup.healthcheck.StartupHealthCheck;
import org.neo4j.server.web.Jetty6WebServer;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * @author mh
 * @since 24.03.11
 */
public class LocalTestServer {
    private NeoServerWithEmbeddedWebServer neoServer;
    private final int port;
    private final String hostname;
    protected String propertiesFile = "test-db.properties";

    public LocalTestServer() {
        this("localhost",7473);
    }

    public LocalTestServer(String hostname, int port) {
        this.port = port;
        this.hostname = hostname;
    }

    public void start() {
        if (neoServer!=null) throw new IllegalStateException("Server already running");
        URL url = getClass().getResource("/" + propertiesFile);
        if (url==null) throw new IllegalArgumentException("Could not resolve properties file "+propertiesFile);
        neoServer = new NeoServerWithEmbeddedWebServer(new AddressResolver() {
            @Override
            public String getHostname() {
                return hostname;
            }
        }, new StartupHealthCheck(), new File(url.getPath()), new Jetty6WebServer()) {
            protected void registerServerModules() {
                registerModule(RESTApiModule.class);
                registerModule(ThirdPartyJAXRSModule.class);
            }

            @Override
            protected int getWebServerPort() {
                return port;
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

    public AbstractGraphDatabase getGraphDatabase() {
        return getDatabase().graph;
    }
}
