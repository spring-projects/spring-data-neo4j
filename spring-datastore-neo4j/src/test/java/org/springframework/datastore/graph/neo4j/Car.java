package org.springframework.datastore.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.annotation.NodeEntity;

@NodeEntity
public abstract class Car {
	public Car() {
	}

	public Car(Node n) {
		setUnderlyingState(n);
	}
}
