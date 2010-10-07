package org.springframework.datastore.graph.api;

import org.neo4j.graphdb.Relationship;

public interface RelationshipBacked extends GraphBacked<Relationship>{
	
	Relationship getUnderlyingState();
	
	void setUnderlyingState(Relationship r);
}
