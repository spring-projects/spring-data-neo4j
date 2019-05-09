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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.cypher.Relationship.Direction;
import org.springframework.data.neo4j.core.cypher.support.Visitable;
import org.springframework.data.neo4j.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/M14/railroad/NodePattern.html">NodePattern</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Node implements PatternElement, Named, Expression, ExposesRelationships<Relationship> {

	static Node create(String primaryLabel, String... additionalLabels) {

		Assert.hasText(primaryLabel, "A primary label is required.");

		for (String additionalLabel : additionalLabels) {
			Assert.hasText(additionalLabel, "An empty label is not allowed.");
		}

		return new Node(primaryLabel, additionalLabels);
	}

	/**
	 * @return A node without labels
	 */
	static Node create() {
		return new Node(null);
	}

	private @Nullable final SymbolicName symbolicName;

	private final List<String> labels;

	Node(String primaryLabel, String... additionalLabels) {

		this.symbolicName = null;

		this.labels = new ArrayList<>();
		if (!(primaryLabel == null || primaryLabel.isEmpty())) {
			this.labels.add(primaryLabel);
		}
		this.labels.addAll(Arrays.asList(additionalLabels));
	}

	Node(SymbolicName symbolicName, List<String> labels) {

		this.symbolicName = symbolicName;

		this.labels = new ArrayList<>(labels);
	}

	/**
	 * Creates a copy of this node with a new symbolic name.
	 *
	 * @param newSymbolicName the new symbolic name.
	 * @return The new node.
	 */
	public Node named(String newSymbolicName) {

		Assert.hasText(newSymbolicName, "Symbolic name is required.");
		return new Node(new SymbolicName(newSymbolicName), labels);
	}

	public Optional<SymbolicName> getSymbolicName() {
		return Optional.ofNullable(symbolicName);
	}

	public List<String> getLabels() {
		return Collections.unmodifiableList(labels);
	}

	public boolean isLabeled() {
		return !this.labels.isEmpty();
	}

	/**
	 * Creates a new {@link Property} associated with this property container..
	 * <p/>
	 * Note: The property container does not track property creation and there is no possibility to enumerate all
	 * properties that have been created for this node.
	 *
	 * @param name property name, must not be {@literal null} or empty.
	 * @return a new {@link Property} associated with this {@link Node}.
	 */
	public Property property(String name) {

		return Property.create(this, name);
	}

	public FunctionInvocation internalId() {
		return Functions.id(this);
	}

	@Override
	public Relationship relationshipTo(Node other, String... types) {
		return Relationship.create(this, Direction.LTR, other, types);
	}

	@Override
	public Relationship relationshipFrom(Node other, String... types) {
		return Relationship.create(this, Direction.RTR, other, types);
	}

	@Override
	public Relationship relationshipBetween(Node other, String... types) {
		return Relationship.create(this, Direction.UNI, other, types);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		Visitable.visitIfNotNull(this.symbolicName, visitor);
		visitor.leave(this);
	}
}
