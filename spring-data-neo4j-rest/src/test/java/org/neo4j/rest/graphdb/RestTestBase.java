package org.neo4j.rest.graphdb;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.server.AddressResolver;
import org.neo4j.server.NeoServerWithEmbeddedWebServer;
import org.neo4j.server.modules.RESTApiModule;
import org.neo4j.server.modules.ThirdPartyJAXRSModule;
import org.neo4j.server.startup.healthcheck.StartupHealthCheck;
import org.neo4j.server.web.Jetty6WebServer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

public class RestTestBase {

    protected RestGraphDatabase graphDb;
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 7473;
    private static LocalTestServer neoServer = new LocalTestServer(HOSTNAME,PORT).withPropertiesFile("test-db.properties");
    private static final String SERVER_ROOT_URI = "http://" + HOSTNAME + ":" + PORT + "/db/data/";

    @BeforeClass
    public static void startDb() throws Exception {
        BasicConfigurator.configure();
        neoServer.start();
    }

    @Before
    public void setUp() throws URISyntaxException {
        cleanDb();
        graphDb = new RestGraphDatabase(new URI(SERVER_ROOT_URI));
    }

    public static void cleanDb() {
        neoServer.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        neoServer.stop();

    }

    protected Relationship relationship() {
        Iterator<Relationship> it = node().getRelationships(Direction.OUTGOING).iterator();
        if (it.hasNext()) return it.next();
        return node().createRelationshipTo(graphDb.createNode(null), Type.TEST);
    }

    protected Node node() {
        return graphDb.getReferenceNode();
    }
}
