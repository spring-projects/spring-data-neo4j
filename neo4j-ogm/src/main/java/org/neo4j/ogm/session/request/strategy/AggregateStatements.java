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

package org.neo4j.ogm.session.request.strategy;

import org.neo4j.ogm.cypher.query.RowModelQuery;

import java.util.Collection;
import java.util.Collections;

/**
 * Encapsulates Cypher statements used to execute aggregation queries.
 *
 * @author Adam George
 */
public class AggregateStatements {

    public RowModelQuery countNodesLabelledWith(Collection<String> labels) {
        StringBuilder cypherLabels = new StringBuilder();
        for (String label : labels) {
            cypherLabels.append(":`").append(label).append('`');
        }
        return new RowModelQuery(String.format("MATCH (n%s) RETURN COUNT(n)", cypherLabels.toString()),
                Collections.<String, String> emptyMap());
    }

}
