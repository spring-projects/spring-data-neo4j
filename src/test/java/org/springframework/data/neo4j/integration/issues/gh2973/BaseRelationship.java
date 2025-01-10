package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public abstract class BaseRelationship {
	@RelationshipId
	@GeneratedValue
	private Long id;
	@TargetNode
	private BaseNode targetNode;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BaseNode getTargetNode() {
		return targetNode;
	}

	public void setTargetNode(BaseNode targetNode) {
		this.targetNode = targetNode;
	}
}
