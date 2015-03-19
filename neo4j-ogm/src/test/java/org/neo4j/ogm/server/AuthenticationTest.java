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

package org.neo4j.ogm.server;

import org.apache.http.client.HttpResponseException;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.Test;
import org.neo4j.ogm.domain.bike.Bike;
import org.neo4j.ogm.integration.LocalhostServerTest;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.result.ResultProcessingException;
import org.neo4j.ogm.session.transaction.Transaction;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Vince Bickers
 */
public class AuthenticationTest extends LocalhostServerTest {

    private Session session;

    private boolean AUTH = true;
    private boolean NO_AUTH = false;

    @Test
    public void testUnauthorizedSession() {

        init(NO_AUTH, "org.neo4j.ogm.domain.bike");

        try ( Transaction tx = session.beginTransaction() ) {
            session.loadAll(Bike.class);
            fail("A non-authenticating version of Neo4j is running. Please start Neo4j 2.2.0 or later to run these tests");
        } catch (ResultProcessingException rpe) {
            Throwable cause = rpe.getCause();
            if (cause instanceof HttpHostConnectException) {
                fail("Please start Neo4j 2.2.0 or later to run these tests");
            } else {
                assertTrue(cause instanceof HttpResponseException);
                assertEquals("Unauthorized", cause.getMessage());
            }
        }

    }

    @Test
    public void testAuthorizedSession() {

        init(AUTH, "org.neo4j.ogm.domain.bike");

        try ( Transaction tx = session.beginTransaction() ) {
            session.loadAll(Bike.class);
        } catch (ResultProcessingException rpe) {
            fail("'" + rpe.getCause().getLocalizedMessage() + "' was not expected here");
        }

    }

    private void init(boolean auth, String... packages){

        if (auth) {
            System.setProperty("username", "neo4j");
            System.setProperty("password", "password");
        } else {
            System.getProperties().remove("username");
            System.getProperties().remove("password");
        }

        try {
            session = super.session(packages);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
