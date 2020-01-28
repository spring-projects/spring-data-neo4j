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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.Relationship.Direction;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/RelationshipDetail.html">RelationshipDetail</a>
 *
 * @author Michael J. Simons
 * @author Philipp Tölle
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class RelationshipDetail implements Visitable {

	/**
	 * The direction between the nodes of the relationship.
	 */
	private final Direction direction;

	private @Nullable final SymbolicName symbolicName;

	private final List<String> types;

	private @Nullable final Properties properties;

	RelationshipDetail(Direction direction,
		@Nullable SymbolicName symbolicName, List<String> types) {
		this(direction, symbolicName, types, null);
	}

	RelationshipDetail(Direction direction,
		@Nullable SymbolicName symbolicName, List<String> types, @Nullable Properties properties) {

		this(direction, symbolicName, properties);

		boolean nullTypePresent = types.stream().filter(type -> type == null || type.isEmpty()).findAny().isPresent();
		Assert.isTrue(!nullTypePresent, "The list of types may not contain literal null or an empty type.");
		this.types.addAll(types);
	}

	private RelationshipDetail(Direction direction,
		@Nullable SymbolicName symbolicName, @Nullable Properties properties) {

		this.direction = direction;
		this.symbolicName = symbolicName;
		this.types = new ArrayList<>();
		this.properties = properties;
	}

	RelationshipDetail named(String newSymbolicName) {

		Assert.hasText(newSymbolicName, "Symbolic name is required.");
		return new RelationshipDetail(this.direction, SymbolicName.create(newSymbolicName), this.types, properties);
	}

	public Direction getDirection() {
		return direction;
	}

	Optional<SymbolicName> getSymbolicName() {
		return Optional.ofNullable(symbolicName);
	}

	public List<String> getTypes() {
		return Collections.unmodifiableList(types);
	}

	public boolean isTyped() {
		return !this.types.isEmpty();
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		Visitable.visitIfNotNull(this.symbolicName, visitor);
		Visitable.visitIfNotNull(this.properties, visitor);
		visitor.leave(this);
	}
}
