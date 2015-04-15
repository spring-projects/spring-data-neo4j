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
 * {@link RowModelMapper} that maps each row value onto
 *
 * @param <T> The type of entity to which the row is to be mapped
 */
class EntityRowModelMapper<T> implements RowModelMapper<T> {

    @Override
    @SuppressWarnings("unchecked")
    public void mapIntoResult(Collection<T> result, Object[] rowValues, String[] responseVariables) {
        if (responseVariables.length > 1) {
            throw new RuntimeException(
                    "Scalar response queries must only return one column. Make sure your cypher query only returns one item.");
        }

        for (int i = 0; i < responseVariables.length; i++) {
            result.add((T) rowValues[i]);
        }
    }

}
