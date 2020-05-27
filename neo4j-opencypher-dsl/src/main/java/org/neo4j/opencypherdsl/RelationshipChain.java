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
package org.neo4j.opencypherdsl;

import static org.apiguardian.api.API.Status.*;
import static org.neo4j.opencypherdsl.support.Visitable.*;

import java.util.LinkedList;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * Represents a chain of relationships. The chain is meant to be in order and the right node of an element is related to
 * the left node of the next element.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class RelationshipChain implements RelationshipPattern, ExposesRelationships<RelationshipChain> {

	private final LinkedList<Relationship> relationships = new LinkedList<>();

	static RelationshipChain create(Relationship firstElement) {

		return new RelationshipChain()
			.add(firstElement);
	}

	RelationshipChain add(Relationship element) {

		Assert.notNull(element, "Elements of a relationship chain must not be null.");
		this.relationships.add(element);
		return this;
	}

	@Override
	public RelationshipChain relationshipTo(Node other, String... types) {
		return this.add(this.relationships.getLast().getRight().relationshipTo(other, types));
	}

	@Override
	public RelationshipChain relationshipFrom(Node other, String... types) {
		return this.add(this.relationships.getLast().getRight().relationshipFrom(other, types));
	}

	@Override
	public RelationshipChain relationshipBetween(Node other, String... types) {
		return this.add(this.relationships.getLast().getRight().relationshipBetween(other, types));
	}

	/**
	 * Replaces the last element of this chains with a copy of the relationship with the new symbolic name.
	 *
	 * @param newSymbolicName The new symbolic name to use
	 * @return This chain
	 */
	public RelationshipChain named(String newSymbolicName) {

		Relationship lastElement = this.relationships.removeLast();
		return this.add(lastElement.named(newSymbolicName));
	}

	/**
	 * Changes the length of the last element of this chain to a new minimum length
	 *
	 * @param minimum the new minimum
	 * @return This chain
	 */
	public RelationshipChain min(Integer minimum) {

		Relationship lastElement = this.relationships.removeLast();
		return this.add(lastElement.min(minimum));
	}

	/**
	 * Changes the length of the last element of this chain to a new maximum length
	 *
	 * @param maximum the new maximum
	 * @return This chain
	 */
	public RelationshipChain max(Integer maximum) {

		Relationship lastElement = this.relationships.removeLast();
		return this.add(lastElement.max(maximum));
	}

	/**
	 * Changes the length of the last element of this chain
	 *
	 * @param minimum the new minimum
	 * @param maximum the new maximum
	 * @return This chain
	 */
	public RelationshipChain length(Integer minimum, Integer maximum) {

		Relationship lastElement = this.relationships.removeLast();
		return this.add(lastElement.length(minimum, maximum));
	}

	/**
	 * Adds properties to the last element of this chain.
	 *
	 * @param newProperties the new properties (can be {@literal null} to remove exiting properties).
	 * @return This chain
	 */
	public RelationshipChain properties(MapExpression<?> newProperties) {

		Relationship lastElement = this.relationships.removeLast();
		return this.add(lastElement.withProperties(newProperties));
	}

	/**
	 * Adds properties to the last element of this chain.
	 *
	 * @param keysAndValues A list of key and values. Must be an even number, with alternating {@link String} and {@link Expression}.
	 * @return This chain
	 */
	public RelationshipChain properties(Object... keysAndValues) {

		Relationship lastElement = this.relationships.removeLast();
		return this.add(lastElement.withProperties(keysAndValues));
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);

		Node lastNode = null;
		for (Relationship relationship : relationships) {

			relationship.getLeft().accept(visitor);
			relationship.getDetails().accept(visitor);

			lastNode = relationship.getRight();
		}

		visitIfNotNull(lastNode, visitor);

		visitor.leave(this);
	}
}
