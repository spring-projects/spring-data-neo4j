/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2905;

import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Mathias Kühn
 */
@Node
public abstract class BugTargetBaseV1 {
	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	protected String uuid;

	private String name;

	@Relationship(type = "RELI", direction = Relationship.Direction.OUTGOING)
	public Set<BugFromV1> relatedBugs;

	BugTargetBaseV1(String uuid, String name, Set<BugFromV1> relatedBugs) {
		this.uuid = uuid;
		this.name = name;
		this.relatedBugs = relatedBugs;
	}
}
