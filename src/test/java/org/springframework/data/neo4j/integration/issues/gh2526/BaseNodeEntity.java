package org.springframework.data.neo4j.integration.issues.gh2526;

import lombok.*;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

@org.springframework.data.neo4j.core.schema.Node
@NonFinal
@Data
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuperBuilder(toBuilder = true)
public class BaseNodeEntity {

	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	@EqualsAndHashCode.Include
	private String nodeId;
}
