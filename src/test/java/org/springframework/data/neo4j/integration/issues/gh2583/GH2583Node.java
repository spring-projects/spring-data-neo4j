/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration.issues.gh2583;

import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * A simple node with bidirectional relationship mapping to the very same type.
 */
@Node
public class GH2583Node {

	@Relationship(type = "LINKED", direction = Relationship.Direction.OUTGOING)
	public List<GH2583Node> outgoingNodes;

	@Relationship(type = "LINKED", direction = Relationship.Direction.INCOMING)
	public List<GH2583Node> incomingNodes;

	@Id
	@GeneratedValue
	Long id;

}
