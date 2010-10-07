package org.springframework.datastore.graph.api;

public enum Direction {
	
	OUTGOING(org.neo4j.graphdb.Direction.OUTGOING), INCOMING(org.neo4j.graphdb.Direction.INCOMING), BOTH(org.neo4j.graphdb.Direction.BOTH);
	
	private org.neo4j.graphdb.Direction neo4jDirection;
	private Direction( org.neo4j.graphdb.Direction neo4jDirection ) {
		this.neo4jDirection = neo4jDirection;
	}
	
	public org.neo4j.graphdb.Direction toNeo4jDir() {
		return this.neo4jDirection;
	}

}
