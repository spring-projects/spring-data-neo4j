package org.springframework.data.neo4j.integration.issues.gh2908;

import org.neo4j.driver.Values;
import org.neo4j.driver.types.Point;

public enum Place {

	NEO4J_HQ(Values.point(4326, 12.994823, 55.612191).asPoint()),
	SFO(Values.point(4326, -122.38681, 37.61649).asPoint()),
	CLARION(Values.point(4326, 12.994243, 55.607726).asPoint()),
	MINC(Values.point(4326, 12.994039, 55.611496).asPoint());

	private final Point value;

	Place(Point value) {
		this.value = value;
	}

	public Point getValue() {
		return value;
	}
}
