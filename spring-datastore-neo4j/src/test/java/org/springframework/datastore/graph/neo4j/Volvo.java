package org.springframework.datastore.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.NodeEntity;

@NodeEntity
public class Volvo extends Car {
	public Volvo() {
	}

	public Volvo(Node n) {
		setUnderlyingState(n);
	}
}
