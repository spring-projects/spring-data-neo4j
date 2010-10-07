package org.springframework.datastore.graph.neo4j;

import org.neo4j.graphdb.Node;

public class Toyota extends Car {
	public Toyota() {

	}

	public Toyota(Node n) {
		setUnderlyingState(n);
	}
}
