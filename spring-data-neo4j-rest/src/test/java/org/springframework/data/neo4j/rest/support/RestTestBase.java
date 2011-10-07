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


import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.rest.SpringRestGraphDatabase;

import java.net.URI;
import java.util.Iterator;

public class RestTestBase {

    protected SpringRestGraphDatabase restGraphDatabase;
    private static final String HOSTNAME = "127.0.0.1";
    public static final int PORT = 7473;
    protected static LocalTestServer neoServer = new LocalTestServer(HOSTNAME,PORT).withPropertiesFile("test-db.properties");
    public static final String SERVER_ROOT_URI = "http://" + HOSTNAME + ":" + PORT + "/db/data/";

    @BeforeClass
    public static void startDb() throws Exception {
        BasicConfigurator.configure();
        neoServer.start();
    }

    @Before
    public void setUp() throws Exception {
        cleanDb();
        restGraphDatabase = new SpringRestGraphDatabase(SERVER_ROOT_URI);
    }

    public static void cleanDb() {
        neoServer.cleanDb();
    }

    @AfterClass
    public static void shutdownDb() {
        neoServer.stop();
    }

    public GraphDatabaseService getGraphDatabase() {
        return neoServer.getGraphDatabase();
    }


    protected Relationship relationship() {
        Iterator<Relationship> it = node().getRelationships(Direction.OUTGOING).iterator();
        if (it.hasNext()) return it.next();
        return node().createRelationshipTo(restGraphDatabase.createNode(null), Type.TEST);
    }

    protected Node node() {
        return restGraphDatabase.getReferenceNode();
    }
}
