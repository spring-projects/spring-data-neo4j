package org.springframework.data.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.annotation.NodeEntity;

@NodeEntity
public abstract class Car {
	public Car() {
	}

	public Car(Node n) {
		setPersistentState(n);
	}
}
