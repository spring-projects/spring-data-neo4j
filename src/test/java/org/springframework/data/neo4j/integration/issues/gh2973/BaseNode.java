package org.springframework.data.neo4j.integration.issues.gh2973;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.OUTGOING;

@Node
public class BaseNode {
	@Id
	@GeneratedValue
	private UUID id;
	@Relationship(direction = OUTGOING)
	private Map<String, List<BaseRelationship>> relationships = new HashMap<>();

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Map<String, List<BaseRelationship>> getRelationships() {
		return relationships;
	}

	public void setRelationships(Map<String, List<BaseRelationship>> relationships) {
		this.relationships = relationships;
	}
}