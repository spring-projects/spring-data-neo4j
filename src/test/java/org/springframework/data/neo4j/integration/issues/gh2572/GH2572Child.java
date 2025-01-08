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
package org.springframework.data.neo4j.integration.issues.gh2572;

import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Michael J. Simons
 */
@Node
public class GH2572Child extends GH2572BaseEntity<GH2572Child> {
	private String name;

	@Relationship(value = "IS_PET", direction = Relationship.Direction.OUTGOING)
	private GH2572Parent owner;

	public GH2572Child(String name, GH2572Parent owner) {
		this.name = name;
		this.owner = owner;
	}

	public GH2572Child() {
	}

	public String getName() {
		return this.name;
	}

	public GH2572Parent getOwner() {
		return this.owner;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setOwner(GH2572Parent owner) {
		this.owner = owner;
	}
}
