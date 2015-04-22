/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.ogm.domain.bike.WheelWithUUID;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Michal Bachman
 */
public class TxHandlerIntegrationTest {

    @Rule
    public Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7577);

    private static SessionFactory sessionFactory;
    private Session session;

    @BeforeClass
    public static void init() throws IOException {
        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.bike");
    }

    @Before
    public void setUp() throws Exception {
        neo4jRule.getGraphDatabaseService().registerTransactionEventHandler(new TransactionEventHandler.Adapter<Object>() {
            @Override
            public Object beforeCommit(TransactionData data) throws Exception {
                for (Node createdNode : data.createdNodes()) {
                    createdNode.setProperty("uuid", UUID.randomUUID().toString());
                }

                return null;
            }
        });
        session = sessionFactory.openSession(neo4jRule.baseNeoUrl());
    }

    @Test
    @Ignore  // FIXME (but how?)
    public void shouldPropagateDatabaseDrivenChangesToObjectGraph() throws InterruptedException {
        WheelWithUUID wheel = new WheelWithUUID();
        wheel.setSpokes(2);

        session.save(wheel);

        long id = wheel.getId();

        String uuid;
        try (Transaction tx = neo4jRule.getGraphDatabaseService().beginTx()) {
            uuid = neo4jRule.getGraphDatabaseService().getNodeById(id).getProperty("uuid", "unknown").toString();
            tx.success();
        }

        assertNotNull(uuid);

        //fails here
        assertEquals(uuid, wheel.getUuid());
    }

}
