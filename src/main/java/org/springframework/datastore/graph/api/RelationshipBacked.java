package org.springframework.datastore.graph.api;

import org.neo4j.graphdb.Relationship;

public interface RelationshipBacked {
	
	Relationship getUnderlyingRelationship();
	
	void setUnderlyingRelationship(Relationship r);
}
