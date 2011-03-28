package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.NodeBacked;

/**
 * @author mh
 * @since 29.03.11
 */
public interface NodeGraphRepository<T extends NodeBacked> extends GraphRepository<Node,T> {
}
