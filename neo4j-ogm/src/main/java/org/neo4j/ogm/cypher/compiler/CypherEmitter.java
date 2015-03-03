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
