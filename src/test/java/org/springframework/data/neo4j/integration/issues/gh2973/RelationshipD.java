package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;

@RelationshipProperties
public class RelationshipD extends BaseRelationship {
	String d;

	public String getD() {
		return d;
	}

	public void setD(String d) {
		this.d = d;
	}
}
