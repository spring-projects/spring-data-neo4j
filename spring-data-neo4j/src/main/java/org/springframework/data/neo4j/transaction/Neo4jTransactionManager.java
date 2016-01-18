/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.transaction;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Vince Bickers
 */
public class Neo4jTransactionManager implements PlatformTransactionManager {

    private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionManager.class);
    private final Session session;

    public Neo4jTransactionManager(Session session) {
        this.session = session;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition transactionDefinition) throws TransactionException {
        logger.debug("Requesting to create or join a transaction");
        return new Neo4jTransactionStatus(session, transactionDefinition);
    }

    @Override
    public void commit(TransactionStatus transactionStatus) throws TransactionException {
        Transaction tx = ((Neo4jTransactionStatus) transactionStatus).getTransaction();
        logger.debug("Commit requested: " + tx + ", status: " + tx.status().toString());
        if (transactionStatus.isNewTransaction()) {
            if (tx.status() == (Transaction.Status.PENDING) || tx.status() == (Transaction.Status.OPEN)) {
                logger.debug("Commit invoked");
                tx.commit();
            }
        } else {
            logger.debug("Commit deferred");
        }
    }

    @Override
    public void rollback(TransactionStatus transactionStatus) throws TransactionException {
        Transaction tx = ((Neo4jTransactionStatus) transactionStatus).getTransaction();
        logger.debug("Rollback requested: " + tx + ", status: " + tx.status().toString());
        if (tx.status() == (Transaction.Status.PENDING) || tx.status() == (Transaction.Status.OPEN)) {
            logger.debug("Rollback invoked");
            tx.rollback();
        }
    }
}
