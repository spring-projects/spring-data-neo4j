package org.springframework.data.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.neo4j.Car;

public class Toyota extends Car {
	public Toyota() {

	}

	public Toyota(Node n) {
		setUnderlyingState(n);
	}
}
