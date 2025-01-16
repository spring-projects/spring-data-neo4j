package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.core.schema.RelationshipProperties;

@RelationshipProperties(persistTypeInfo = true)
public class RelationshipA extends BaseRelationship {
	String a;

	public String getA() {
		return a;
	}

	public void setA(String a) {
		this.a = a;
	}
}
