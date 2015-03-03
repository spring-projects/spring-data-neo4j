/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.ogm.domain.bike.WheelWithUUID;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TxHandlerIntegrationTest extends WrappingServerIntegrationTest {

    private static SessionFactory sessionFactory;
    private Session session;

    @BeforeClass
    public static void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.bike");
    }

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = sessionFactory.openSession(baseNeoUrl());
    }

    @Test
    @Ignore  // FIXME (but how?)
    public void shouldPropagateDatabaseDrivenChangesToObjectGraph() throws InterruptedException {
        WheelWithUUID wheel = new WheelWithUUID();
        wheel.setSpokes(2);

        session.save(wheel);

        long id = wheel.getId();

        String uuid;
        try (Transaction tx = getDatabase().beginTx()) {
            uuid = getDatabase().getNodeById(id).getProperty("uuid", "unknown").toString();
            tx.success();
        }

        assertNotNull(uuid);

        //fails here
        assertEquals(uuid, wheel.getUuid());
    }

    @Override
    protected GraphDatabaseService createDatabase() {
        GraphDatabaseService database = super.createDatabase();

        database.registerTransactionEventHandler(new TransactionEventHandler.Adapter<Object>() {
            @Override
            public Object beforeCommit(TransactionData data) throws Exception {
                for (Node createdNode : data.createdNodes()) {
                    createdNode.setProperty("uuid", UUID.randomUUID().toString());
                }

                return null;
            }
        });

        return database;
    }

    @Override
    protected int neoServerPort() {
        return 7577;
    }
}
