/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.neo4j.ogm.session;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.session.request.RequestHandler;
import org.neo4j.ogm.session.transaction.Transaction;

/**
 * A callback interface used to pass functional code to the {@link Session} to be executed in a transaction
 * and with access to the underlying OGM meta-data.
 *
 * @param <T> The type of object returned from applying this callback
 * @author Adam George
 */
public interface GraphCallback<T> {

    /**
     * Called by the OGM {@link Session} in a transaction to perform some arbitrary database operation.
     *
     * @param requestHandler The {@link RequestHandler} for communication with the database
     * @param transaction The {@link Transaction} in which the database communication is taking place
     * @param metaData The mapping {@link MetaData} that pertains to the current session
     * @return An arbitrary result (or <code>null</code>) based on the desired behaviour of this callback function
     */
    T apply(RequestHandler requestHandler, Transaction transaction, MetaData metaData);

}
