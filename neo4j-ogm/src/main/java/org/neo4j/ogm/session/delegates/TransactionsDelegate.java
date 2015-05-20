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
package org.neo4j.ogm.session.delegates;

import org.neo4j.ogm.session.Capability;
import org.neo4j.ogm.session.GraphCallback;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.transaction.SimpleTransaction;
import org.neo4j.ogm.session.transaction.Transaction;

/**
 * @author: Vince Bickers
 */
public class TransactionsDelegate implements Capability.Transactions {

    private final Neo4jSession session;
    private String autoCommitUrl;

    public TransactionsDelegate(Neo4jSession neo4jSession) {
        this.session = neo4jSession;
    }

    public void autoCommit(String url) {
        if (url == null) return;
        if (!url.endsWith("/")) url = url + "/";
        autoCommitUrl = url + "db/data/transaction/commit";
    }

    @Override
    public Transaction beginTransaction() {

        session.info("beginTransaction() being called on thread: " + Thread.currentThread().getId());
        session.info("Neo4jSession identity: " + this);

        Transaction tx = session.transactionManager().openTransaction(session.context());

        session.info("Obtained new transaction: " + tx.url() + ", tx id: " + tx);
        return tx;
    }

    @Override
    public <T> T doInTransaction(GraphCallback<T> graphCallback) {
        return graphCallback.apply(session.requestHandler(), getCurrentOrAutocommitTransaction(), session.metaData());
    }


    @Override
    public Transaction getTransaction() {
        return session.transactionManager().getCurrentTransaction();
    }

    public Transaction getCurrentOrAutocommitTransaction() {

        session.info("--------- new request ----------");
        session.info("getOrCreateTransaction() being called on thread: " + Thread.currentThread().getId());
        session.info("Session identity: " + this);

        Transaction tx = session.transactionManager().getCurrentTransaction();
        if (tx == null
                || tx.status().equals(Transaction.Status.CLOSED)
                || tx.status().equals(Transaction.Status.COMMITTED)
                || tx.status().equals(Transaction.Status.ROLLEDBACK)) {
            session.info("There is no existing transaction, creating a transient one");
            return new SimpleTransaction(session.context(), autoCommitUrl);
        }

        session.info("Current transaction: " + tx.url() + ", tx id: " + tx);
        return tx;

    }


}
