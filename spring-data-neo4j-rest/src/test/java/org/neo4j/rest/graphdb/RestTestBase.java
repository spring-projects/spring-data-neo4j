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


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.*;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.harness.internal.InProcessServerControls;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.rest.graphdb.entity.RestNode;
import org.neo4j.rest.graphdb.util.Config;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.tooling.GlobalGraphOperations;

import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

public class RestTestBase {

    private GraphDatabaseService restGraphDb;
    private static String HOSTNAME = "localhost";
    private static int PORT = 7473;
    private static ServerControls neoServer;
    public static String SERVER_ROOT = "http://" + HOSTNAME + ":" + PORT;
    protected static String SERVER_ROOT_URI = SERVER_ROOT + "/db/data/";
    private long referenceNodeId;
    private Node referenceNode;
    private static AbstractNeoServer server;

    static {
        initServer();
    }
    protected static void initServer() {
        try {
            if (neoServer != null) {
                neoServer.close();
            }
            neoServer = TestServerBuilders.newInProcessBuilder()
                    .withConfig("dbms.security.auth_enabled", "false")
                    .withExtension("/test", "org.springframework.data.neo4j.rest.support")
                    .newServer();

            Field field = InProcessServerControls.class.getDeclaredField("server");
            field.setAccessible(true);
            server = (AbstractNeoServer) field.get(neoServer);
            SERVER_ROOT = neoServer.httpURI().toString();
            SERVER_ROOT = SERVER_ROOT.substring(0, SERVER_ROOT.length() - 1);
            SERVER_ROOT_URI = SERVER_ROOT + "/db/data/";
            HOSTNAME = neoServer.httpURI().getHost();
            PORT = neoServer.httpURI().getPort();
        } catch (Exception e) {
            throw new RuntimeException("Error starting in-process server",e);
        }
    }

    @BeforeClass
    public static void startDb() throws Exception {
        tryConnect();
    }

    private static void tryConnect() throws InterruptedException {
        int retryCount = 3;
        for (int i = 0; i < retryCount; i++) {
            try {
                RequestResult result = new ExecutingRestRequest(SERVER_ROOT_URI).get("");
                assertEquals(200, result.getStatus());
                System.err.println("Successful HTTP connection to "+SERVER_ROOT_URI);
                return;
            } catch (Exception e) {
                System.err.println("Error retrieving ROOT URI " + e.getMessage());
                Thread.sleep(500);
            }
        }
    }

    @Before
    public void setUp() throws Exception {
        System.setProperty(Config.CONFIG_BATCH_TRANSACTION,"false");
        new Neo4jDatabaseCleaner(server.getDatabase().getGraph()).cleanDb();
//        server.getDatabase().getGraph().cleanDb();
        restGraphDb = createRestGraphDatabase();

        GraphDatabaseService db = getGraphDatabase();
        try (Transaction tx = db.beginTx()) {
            Node node = db.createNode();
            this.referenceNodeId = node.getId();
            tx.success();
        }
        this.referenceNode = restGraphDb.getNodeById(referenceNodeId);

    }

    protected GraphDatabaseService createRestGraphDatabase() {
        return new CypherRestGraphDatabase(SERVER_ROOT_URI);
    }

    @After
    public void tearDown() throws Exception {
        restGraphDb.shutdown();
    }

    @AfterClass
    public static void shutdownDb() {
//        neoServer.close();
//        neoServer = null;
    }

    protected Relationship relationship() {
        Iterator<Relationship> it = node().getRelationships(Direction.OUTGOING).iterator();
        if (it.hasNext()) return it.next();
        return node().createRelationshipTo(restGraphDb.createNode(), Type.TEST);
    }

    protected Node node() {
        return referenceNode;
    }
    protected long nodeId() {
        return referenceNodeId;
    }

    protected GraphDatabaseService getGraphDatabase() {
    	return server.getDatabase().getGraph();
    }

	protected GraphDatabaseService getRestGraphDb() {
		return restGraphDb;
	}

    protected int countExistingNodes() {
        return IteratorUtil.count(GlobalGraphOperations.at(getGraphDatabase()).getAllNodes());
    }

    protected Node loadRealNode(Node node) {
        return getGraphDatabase().getNodeById(node.getId());
    }
    public String getUserAgent() {
        return null; // todo install filter
//        return neoServer.getUserAgent();
    }
}
