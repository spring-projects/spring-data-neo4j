package org.springframework.persistence.graph.neo4j;

import org.neo4j.graphdb.Node;

/**
 * Interface introduced to objects annotated with GraphEntity 
 * annotation, to hold underlying Neo4j Node state.
 * @author Rod Johnson
 */
public interface NodeBacked {
	
	Node getUnderlyingNode();

}
