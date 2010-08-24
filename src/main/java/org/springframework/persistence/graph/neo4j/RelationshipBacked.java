package org.springframework.persistence.graph.neo4j;

import org.neo4j.graphdb.Relationship;

public interface RelationshipBacked {
	
	Relationship getUnderlyingRelationship();
	
	void setUnderlyingRelationship(Relationship r);
}
