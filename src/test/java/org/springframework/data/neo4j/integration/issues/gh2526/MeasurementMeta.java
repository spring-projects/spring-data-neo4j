package org.springframework.data.neo4j.integration.issues.gh2526;

import java.util.Set;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.Relationship;

import static org.springframework.data.neo4j.core.schema.Relationship.Direction.INCOMING;

@org.springframework.data.neo4j.core.schema.Node
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@SuperBuilder(toBuilder = true)
public class MeasurementMeta extends BaseNodeEntity {

	@Relationship(type = "IS_MEASURED_BY", direction = INCOMING)
	private Set<DataPoint> dataPoints;
}
