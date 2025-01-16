package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;

@RelationshipProperties
public class RelationshipC extends BaseRelationship {
	String c;

	public String getC() {
		return c;
	}

	public void setC(String c) {
		this.c = c;
	}
}
