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

/**
 * Manages identifiers used within the scope of a single Cypher query.
 *
 * Two different formatting schemes are used.
 *
 * 1. References to new nodes are identified by a monotonically increasing integer 0, 1, 2...
 * prepended by an underscore, e.g.
 *
 * _0, _1, _2 ...
 *
 * 2. References to existing nodes are identified using the node id prepended by a $, e.g
 *
 * $513400, $9075, $2 ...
 *
 * The use of two separate schemes ensures that the identifiers for new nodes and existing nodes cannot
 * overlap.
 *
 */
class IdentifierManager {

    private static final String NEW_FORMAT = "_%d";
    private static final String EXISTING_FORMAT = "$%d";

    private int idCounter;

    /**
     * Generates the next variable name to use in the context of a Cypher query for creating new objects.
     *
     * @return The next variable name to use of the form _id, never <code>null</code>
     */
    public synchronized String nextIdentifier() {
        return String.format(NEW_FORMAT, this.idCounter++);
    }

    /**
     * Generates a variable name to use in the context of a Cypher query referring to existing objects.
     *
     * @return The variable name to use of the form $id, never <code>null</code>
     */
    public String identifier(Long value) {
        return String.format(EXISTING_FORMAT, value);
    }

    public synchronized void releaseIdentifier() {
        this.idCounter--;
    }
}
