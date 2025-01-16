package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;

@RelationshipProperties(persistTypeInfo = true)
public class RelationshipB extends BaseRelationship {
	String b;

	public String getB() {
		return b;
	}

	public void setB(String b) {
		this.b = b;
	}
}
