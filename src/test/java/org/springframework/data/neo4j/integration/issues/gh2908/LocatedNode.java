package org.springframework.data.neo4j.integration.issues.gh2908;

import org.neo4j.driver.types.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class LocatedNode {

	@Id
	@GeneratedValue
	private String id;

	private final String name;

	private final Point place;

	public LocatedNode(String name, Point place) {
		this.name = name;
		this.place = place;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Point getPlace() {
		return place;
	}
}
