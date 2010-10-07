package org.springframework.datastore.graph.neo4j.template.util;

import org.neo4j.graphdb.Node;

public interface NodeEvaluator {
    boolean accept(final Node node);
}
