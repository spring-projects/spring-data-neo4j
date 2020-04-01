/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.cypher;

import static org.apiguardian.api.API.Status.*;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * A property that belongs to a property container (either Node or Relationship).
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Property implements Expression {

	static Property create(Node parentContainer, String name) {

		Assert.isTrue(parentContainer.getSymbolicName().isPresent(),
			"A property derived from a node needs a parent with a symbolic name.");
		Assert.hasText(name, "The properties name is required.");

		return new Property(parentContainer.getSymbolicName().get(), new PropertyLookup((name)));
	}

	static Property create(Expression container, String name) {

		Assert.notNull(container, "The property container is required.");
		Assert.hasText(name, "The properties name is required.");

		return new Property(container, new PropertyLookup(name));

	}

	/**
	 * The expression describing the container.
	 */
	private final Expression container;

	/**
	 * The name of this property.
	 */
	private final PropertyLookup name;

	Property(Expression container, PropertyLookup name) {

		this.container = container;
		this.name = name;
	}

	public PropertyLookup getName() {
		return name;
	}

	/**
	 * Creates an {@link Operation} setting this property to a new value. The property does not track the operations
	 * created with this method.
	 *
	 * @param expression expression describing the new value
	 * @return A new operation.
	 */
	public Operation to(Expression expression) {
		return Operations.set(this, expression);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		this.container.accept(visitor);
		this.name.accept(visitor);
		visitor.leave(this);
	}
}
