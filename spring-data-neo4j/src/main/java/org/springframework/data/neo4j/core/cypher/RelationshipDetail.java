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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.core.cypher.Relationship.Direction;
import org.springframework.data.neo4j.core.cypher.support.Visitable;
import org.springframework.lang.Nullable;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/M14/railroad/RelationshipDetail.html">RelationshipDetail</a>
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public class RelationshipDetail implements Visitable {

	/**
	 * The direction between the nodes of the relationship.
	 */
	private final Direction direction;

	private @Nullable final SymbolicName symbolicName;

	private final List<String> types;

	RelationshipDetail(Direction direction,
		@Nullable SymbolicName symbolicName, List<String> types) {
		this.direction = direction;
		this.symbolicName = symbolicName;
		this.types = new ArrayList<>(types);
	}

	public Direction getDirection() {
		return direction;
	}

	public Optional<SymbolicName> getSymbolicName() {
		return Optional.ofNullable(symbolicName);
	}

	public List<String> getTypes() {
		return Collections.unmodifiableList(types);
	}

	public boolean isTyped() {
		return !this.types.isEmpty();
	}
}
