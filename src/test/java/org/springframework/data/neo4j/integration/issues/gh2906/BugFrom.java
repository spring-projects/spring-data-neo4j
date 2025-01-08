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
package org.springframework.data.neo4j.integration.issues.gh2906;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

/**
 * @author Mathias Kühn
 */
@Node
public class BugFrom {
	@Id
	@GeneratedValue(UUIDStringGenerator.class)
	public String uuid;

	String name;

	@Relationship(type = "RELI", direction = Relationship.Direction.INCOMING)
	public IncomingBugRelationship reli;

	@PersistenceCreator // Due to the cyclic mapping you cannot have the relation as constructor parameter, how should this work?
	BugFrom(String name, String uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	public BugFrom(String name, String comment, BugTargetBase target) {
		this.name = name;

		this.reli = new IncomingBugRelationship(comment, target);
	}


	@Override
	public String toString() {
		return String.format("<BugFrom> {uuid: %s, name: %s}", uuid, name);
	}
}

