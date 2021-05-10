/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * Has an assigned id.
 *
 * @author Michael J. Simons
 */
@Node("Thing")
public class ThingWithAssignedId extends AbstractNamedThing {

	@Id private final String theId;

	@Relationship("Has") private List<AnotherThingWithAssignedId> things;

	public ThingWithAssignedId(String theId, String name) {
		this.theId = theId;
		super.setName(name);
	}

	public String getTheId() {
		return theId;
	}

	public List<AnotherThingWithAssignedId> getThings() {
		return things;
	}

	public void setThings(List<AnotherThingWithAssignedId> things) {
		this.things = things;
	}
}
