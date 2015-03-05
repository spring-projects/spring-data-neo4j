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

package org.neo4j.ogm.session.transaction;

import org.neo4j.ogm.cypher.compiler.CypherContext;

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
