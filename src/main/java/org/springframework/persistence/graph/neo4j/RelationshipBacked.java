package org.springframework.persistence.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public interface RelationshipBacked {
	
	Relationship getUnderlyingRelationship();
	
	void setUnderlyingRelationship(Relationship r);
	
	void setUnderlyingStartNode(Node n);
	
	Node getUnderlyingStartNode();
	
	void setUnderlyingEndNode(Node n);
	
	Node getUnderlyingEndNode();

}
