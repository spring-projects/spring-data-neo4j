package org.springframework.data.neo4j.configuration;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.drivers.http.driver.HttpDriver;
import org.neo4j.ogm.service.Components;
import org.neo4j.ogm.testutil.AuthenticatingTestServer;
import org.neo4j.ogm.testutil.TestServer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.server.InProcessServer;
import org.springframework.data.neo4j.server.Neo4jServer;

/**
 * @author vince
 */
public abstract class HttpDriverConfiguration extends Neo4jConfiguration {

    Configuration configuration = new Configuration();

    public HttpDriverConfiguration() {
        configuration.set("neo4j.version","2.3");
        configuration.set("compiler","org.neo4j.ogm.compiler.MultiStatementCypherCompiler");
        configuration.set("driver","org.neo4j.ogm.drivers.http.driver.HttpDriver");
    }

    GraphDatabaseService db = null;
   /* @Bean
    public Neo4jServer neo4jServer() {
        DriverConfiguration configuration = new DriverConfiguration();
        configuration.setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver");
        Neo4jServer server = null;
        if (Components.driver() instanceof HttpDriver) {
            if (Components.neo4jVersion() < 2.2) {
                server =  new InProcessServer(new TestServer());
                configuration.setURI(server.url());
            } else {
                server = new InProcessServer(new AuthenticatingTestServer());
                configuration.setURI(server.url());
                configuration.setCredentials(server.username(),server.password());
            }
        }
        Components.driver().configure(configuration);
        return server;
    }*/



    @Bean
    public GraphDatabaseService graphDatabaseService() {
        return db;
    }

    @Override
    public Driver driver() {
        DriverConfiguration configuration = new DriverConfiguration();
        configuration.setDriverClassName("org.neo4j.ogm.drivers.http.driver.HttpDriver");
        Neo4jServer server = null;
        if (Components.driver() instanceof HttpDriver) {
            if (Components.neo4jVersion() < 2.2) {
                server =  new InProcessServer(new TestServer());
                configuration.setURI(server.url());
            } else {
                server = new InProcessServer(new AuthenticatingTestServer());
                configuration.setURI(server.url());
                configuration.setCredentials(server.username(),server.password());
            }
        }
        db = ((InProcessServer) server).database();
        return new HttpDriver(configuration);
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
