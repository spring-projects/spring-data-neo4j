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
@EqualsAndHashCode
@Immutable
public class Variable {
	@RelationshipId
	Long id;

	@TargetNode
	MeasurementMeta measurement;

	String variable;

	public static Variable create(MeasurementMeta measurement, String variable) {
		return new Variable(null, measurement, variable);
	}

	@Override
	public String toString() {
		return variable + ": " + measurement.getNodeId();
	}
}
