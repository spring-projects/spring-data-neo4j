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

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.Relationship.Direction;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/NodePattern.html">NodePattern</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Node implements PatternElement, Named, Expression, ExposesRelationships<Relationship> {

	static Node create(String primaryLabel, String... additionalLabels) {

		return create(primaryLabel, null, additionalLabels);
	}

	static Node create(String primaryLabel, MapExpression properties, String... additionalLabels) {

		Assert.hasText(primaryLabel, "A primary label is required.");

		for (String additionalLabel : additionalLabels) {
			Assert.hasText(additionalLabel, "An empty label is not allowed.");
		}

		return new Node(primaryLabel, properties == null ? null : new Properties(properties), additionalLabels);
	}

	/**
	 * @return A node without labels and properties
	 */
	static Node create() {
		return new Node(null, null);
	}

	private @Nullable final SymbolicName symbolicName;

	private final List<NodeLabel> labels;

	private @Nullable final Properties properties;

	private Node(String primaryLabel, Properties properties, String... additionalLabels) {

		this.symbolicName = null;

		this.labels = new ArrayList<>();
		if (!(primaryLabel == null || primaryLabel.isEmpty())) {
			this.labels.add(new NodeLabel(primaryLabel));
		}
		this.labels.addAll(Arrays.stream(additionalLabels).map(NodeLabel::new).collect(toList()));
		this.properties = properties;
	}

	private Node(SymbolicName symbolicName, Properties properties, List<NodeLabel> labels) {

		this.symbolicName = symbolicName;

		this.labels = new ArrayList<>(labels);
		this.properties = properties;
	}

	/**
	 * Creates a copy of this node with a new symbolic name.
	 *
	 * @param newSymbolicName the new symbolic name.
	 * @return The new node.
	 */
	public Node named(String newSymbolicName) {

		Assert.hasText(newSymbolicName, "Symbolic name is required.");
		return new Node(SymbolicName.create(newSymbolicName), properties, labels);
	}

	/**
	 * Creates a a copy of this node with additional properties. Creates a node without properties when no properties
	 * * are passed to this method.
	 *
	 * @param newProperties the new properties
	 * @return The new node.
	 */
	public Node properties(@Nullable MapExpression<?> newProperties) {

		return new Node(this.symbolicName, newProperties == null ? null : new Properties(newProperties), labels);
	}

	/**
	 * Creates a a copy of this node with additional properties. Creates a node without properties when no properties
	 * are passed to this method.
	 *
	 * @param keysAndValues A list of key and values. Must be an even number, with alternating {@link String} and {@link Expression}.
	 * @return The new node.
	 */
	public Node properties(Object... keysAndValues) {

		MapExpression<?> newProperties = null;
		if (keysAndValues != null && keysAndValues.length != 0) {
			newProperties = MapExpression.create(keysAndValues);
		}
		return properties(newProperties);
	}

	/**
	 * A list will never be a valid entry for a map projection, so this convenient method prevents trying to create one
	 * from a list of objects. It will delegate to {@link #project(Object...)} with the content of the list.
	 *
	 * @param entries A list of entries for the projection
	 * @return A map projection.
	 */
	public MapProjection project(List<?> entries) {
		return project(entries.toArray());
	}

	/**
	 * Creates a map projection based on this node. The node needs a symbolic name for this to work.
	 * <p>
	 * Entries of type {@code String} in {@code entries} followed by an {@link Expression} will be treated as map keys
	 * pointing to the expression in the projection, {@code String} entries alone will be treated as property lookups on the node.
	 *
	 * @param entries A list of entries for the projection
	 * @return A map projection.
	 */
	public MapProjection project(Object... entries) {
		return MapProjection.create(this.getSymbolicName().orElseThrow(() -> new IllegalStateException("Cannot project a node without a symbolic name.")), entries);
	}

	/**
	 * @return The optional symbolic name of this node.
	 */
	public Optional<SymbolicName> getSymbolicName() {
		return Optional.ofNullable(symbolicName);
	}

	/**
	 * Creates a new {@link Property} associated with this property container..
	 * <p>
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

	/**
	 * A condition that checks for the presence of labels on a node.
	 *
	 * @param labelsToQuery A list of labels to query
	 * @return A condition that checks whether this node has all of the labels to query
	 */
	public Condition hasLabels(String... labelsToQuery) {
		return HasLabelCondition.create(this.getSymbolicName()
				.orElseThrow(() -> new IllegalStateException("Cannot query a node without a symbolic name.")),
			labelsToQuery);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		Visitable.visitIfNotNull(this.symbolicName, visitor);
		this.labels.forEach(label -> label.accept(visitor));
		Visitable.visitIfNotNull(this.properties, visitor);
		visitor.leave(this);
	}

	@Override
	public String toString() {
		return "Node{" +
			"symbolicName=" + symbolicName +
			", labels=" + labels +
			'}';
	}
}
