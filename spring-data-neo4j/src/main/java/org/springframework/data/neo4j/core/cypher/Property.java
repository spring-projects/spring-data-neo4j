/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.cypher;

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * A property that belongs to a property container (either Node or Relationship).
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Property implements Expression {

	static Property create(Node parentContainer, String name) {

		Assert.state(parentContainer.getSymbolicName().isPresent(),
			"A property derived from a node needs a parent with a symbolic name.");
		Assert.hasText(name, "The properties name is required.");

		return new Property(parentContainer, name);
	}

	/**
	 * The property container this property belongs to.
	 */
	private final Node parentContainer;

	/**
	 * The name of this property.
	 */
	private final String name;

	Property(Node parentContainer, String name) {

		this.parentContainer = parentContainer;
		this.name = name;
	}

	public String getParentAlias() {
		return parentContainer.getSymbolicName().get().getName();
	}

	public String getName() {
		return name;
	}

	public Condition matches(String s) {

		return Conditions.matches(this, Cypher.literalOf(s));
	}

	/**
	 * The property does not track the condition created here.
	 *
	 * @return A condition based on this property that evaluates to true when this property is null
	 */
	public Condition isNull() {

		return Conditions.isNull(this);
	}

	/**
	 * The property does not track the condition created here.
	 *
	 * @return A condition based on this property that evaluates to true when this property is not null
	 */
	public Condition isNotNull() {

		return Conditions.isNotNull(this);
	}

	/**
	 * The property does not track the sort items created here.
	 *
	 * @return A sort item for this property in descending order
	 */
	public SortItem descending() {

		return SortItem.create(this, SortItem.Direction.DESC);
	}

	/**
	 * The property does not track the sort items created here.
	 *
	 * @return A sort item for this property in ascending order
	 */
	public SortItem ascending() {

		return SortItem.create(this, SortItem.Direction.ASC);
	}
}
