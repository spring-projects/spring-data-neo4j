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

import java.util.Collection;

/**
 * Utility to map a row-based graph result onto an object.
 *
 * @param <T> The type of object onto which the row model should be mapped
 */
interface RowModelMapper<T> {

    /**
     * Appends elements to the given result for each of the row values.
     *
     * @param result The collection into which mapped objects are to be added
     * @param rowValues The value in the row returned from the graph database
     * @param responseColumns The names of the columns in the row
     */
    void mapIntoResult(Collection<T> result, Object[] rowValues, String[] responseColumns);

}
