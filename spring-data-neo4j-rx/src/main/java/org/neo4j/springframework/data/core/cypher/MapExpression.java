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

import static org.neo4j.springframework.data.core.cypher.Expressions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.TypedSubtree;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.springframework.util.Assert;

/**
 * A dedicated map expression.<p>
 * Most of the comparision methods on this expression will not result in a sensible query fragment.
 * A {@link MapExpression} is be useful as a concrete parameter to functions or as properties on {@link Node nodes}
 * or {@link Relationship relationships}.
 *
 * @author Michael J. Simons
 * @param <S> type of properties or parameter
 * @soundtrack Rammstein - RAMMSTEIN
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class MapExpression<S extends MapExpression<S>> extends TypedSubtree<MapEntry, S> implements Expression {

	static MapExpression<?> create(Object... input) {

		Assert.isTrue(input.length % 2 == 0, "Need an even number of input parameters");
		List<MapEntry> newContent = new ArrayList<>(input.length / 2);
		Set<String> knownKeys = new HashSet<>();

		for (int i = 0; i < input.length; i += 2) {
			Assert.isInstanceOf(String.class, input[i], "Key needs to be of type String.");
			Assert.isInstanceOf(Expression.class, input[i + 1], "Value needs to be of type Expression.");
			Assert.isTrue(!knownKeys.contains(input[i]), "Duplicate key '" + input[i] + "'");

			final KeyValueMapEntry entry = new KeyValueMapEntry((String) input[i], (Expression) input[i + 1]);
			newContent.add(entry);
			knownKeys.add(entry.getKey());
		}

		return new MapExpression<>(newContent);
	}

	static MapExpression<?> withEntries(List<MapEntry> entries) {
		return new MapExpression<>(entries);
	}

	private MapExpression(List<MapEntry> children) {
		super(children);
	}

	MapExpression<?> addEntries(List<MapEntry> entries) {
		List<MapEntry> newContent = new ArrayList<>(super.children.size() + entries.size());
		newContent.addAll(super.children);
		newContent.addAll(entries);
		return new MapExpression<>(newContent);
	}

	@Override
	protected Visitable prepareVisit(MapEntry child) {
		return child instanceof Expression ? nameOrExpression((Expression) child) : child;
	}
}
