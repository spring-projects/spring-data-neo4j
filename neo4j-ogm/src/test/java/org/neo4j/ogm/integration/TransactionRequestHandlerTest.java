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

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Test;
import org.neo4j.ogm.session.transaction.TransactionManager;
import org.neo4j.ogm.session.transaction.Transaction;

import static org.junit.Assert.assertEquals;

/**
 * @author Michal Bachman
 */
public class TransactionRequestHandlerTest extends InMemoryServerTest {

    private static final CloseableHttpClient httpClient = HttpClients.createDefault();

    @Test
    public void testCreateLongTransaction() {

        TransactionManager txRequestHandler = new TransactionManager(httpClient, "http://localhost:" + neoPort);
        try (Transaction tx = txRequestHandler.openTransaction(null)) {
            assertEquals(Transaction.Status.OPEN, tx.status());
        }
    }

    @Test
    public void testCreateConcurrentTransactions() {

        TransactionManager txRequestHandler = new TransactionManager(httpClient, "http://localhost:" + neoPort);

        // note that the try-with-resources implies these transactions are nested, but they are in fact independent
        try (Transaction tx1 = txRequestHandler.openTransaction(null)) {
            try (Transaction tx2 = txRequestHandler.openTransaction(null)) {
                assertEquals(Transaction.Status.OPEN, tx1.status());
                assertEquals(Transaction.Status.OPEN, tx2.status());
            }
        }
    }

}
