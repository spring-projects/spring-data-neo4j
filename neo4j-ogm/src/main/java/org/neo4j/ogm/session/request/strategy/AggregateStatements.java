package org.neo4j.ogm.session.request.strategy;

import java.util.Collection;
import java.util.Collections;

import org.neo4j.ogm.cypher.query.RowModelQuery;

/**
 * Encapsulates Cypher statements used to execute aggregation queries.
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
