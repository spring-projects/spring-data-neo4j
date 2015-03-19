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

package org.neo4j.ogm.cypher.compiler;

import java.util.Map;
import java.util.Set;

public interface CypherEmitter {

    /**
     * Emits one or more Cypher clauses.
     *
     * @param queryBuilder The {@code StringBuilder} to which the Cypher should be appended
     * @param parameters A {@link Map} to which Cypher parameter values may optionally be added as the query is built up
     * @param varStack The variable stack carried through the query, to which this emitter's variable name may be added
     */

    boolean emit(StringBuilder queryBuilder, Map<String, Object> parameters, Set<String> varStack);
}
