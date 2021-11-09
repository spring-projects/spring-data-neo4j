package org.springframework.data.neo4j.integration.issues.gh2526;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
@Value
@AllArgsConstructor
@Immutable
public class Measurand {

	@Id
	String measurandId;
}
