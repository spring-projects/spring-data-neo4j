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

package org.neo4j.ogm.session.transaction;

import org.neo4j.ogm.cypher.compiler.CypherContext;

/**
 * @author Vince Bickers
 */
public interface Transaction extends AutoCloseable {


    /**
     * Adds a new cypher context to this transaction
     * @param context The CypherContext that forms part of this transaction when committed
     */
    void append(CypherContext context);

    /**
     * The endpoint for this transaction
     * @return
     */
    String url();

    /*
     * rollback a transaction that has pending writes
     * calling rollback on a transaction with no pending read/writes is an error
     */
    void rollback();

    /*
     * commit a transaction that has pending writes
     * calling commit on a transaction with no pending read/writes is an error
     */
    void commit();

    /**
     * return the status of the current transaction
     * @return the Status value associated with the current transaction
     */
    Status status();

    public enum Status {
        OPEN, PENDING, ROLLEDBACK, COMMITTED, CLOSED
    }

    void close();
}
