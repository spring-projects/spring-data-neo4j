package org.springframework.data.neo4j.integration.issues.gh2526;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Value
@With
@AllArgsConstructor
@Immutable
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DataPoint {

	@RelationshipId
	Long id;

	boolean manual;

	@TargetNode
	@EqualsAndHashCode.Include
	Measurand measurand;
}
