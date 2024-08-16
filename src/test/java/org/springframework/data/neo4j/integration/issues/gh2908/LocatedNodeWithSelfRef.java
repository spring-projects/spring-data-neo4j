/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2908;

import org.neo4j.driver.types.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * A located node with circles.
 * @author Michael J. Simons
 */
@Node
public class LocatedNodeWithSelfRef implements HasNameAndPlace {

	@Id
	@GeneratedValue
	private String id;

	private final String name;

	private final Point place;

	@Relationship
	private LocatedNodeWithSelfRef next;

	public LocatedNodeWithSelfRef(String name, Point place) {
		this.name = name;
		this.place = place;
	}

	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Point getPlace() {
		return place;
	}

	public LocatedNodeWithSelfRef getNext() {
		return next;
	}

	public void setNext(LocatedNodeWithSelfRef next) {
		this.next = next;
	}
}
