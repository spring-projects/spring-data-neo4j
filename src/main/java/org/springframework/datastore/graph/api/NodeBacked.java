package org.springframework.datastore.graph.api;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;

/**
 * Interface introduced to objects annotated with GraphEntity 
 * annotation, to hold underlying Neo4j Node state.
 * @author Rod Johnson
 */
public interface NodeBacked {
	
	Node getUnderlyingNode();
	
	void setUnderlyingNode(Node n);
	
	Relationship relateTo(NodeBacked nb, RelationshipType type);

}
