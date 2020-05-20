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
import static org.neo4j.opencypherdsl.Expressions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * Represents a map projection as described <a href="https://medium.com/neo4j/loading-graph-data-for-an-object-graph-mapper-or-graphql-5103b1a8b66e">here</a>.
 *
 * @author Michael J. Simons
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class MapProjection implements Expression {

	private SymbolicName name;

	private MapExpression<?> map;

	static MapProjection create(SymbolicName name, Object... content) {

		return new MapProjection(name, MapExpression.withEntries(createNewContent(content)));
	}

	MapProjection(SymbolicName name, MapExpression<?> map) {
		this.name = name;
		this.map = map;
	}

	/**
	 * Adds additional content. The current projection is left unchanged and a new one is returned.
	 *
	 * @param content The additional content for a new projection.
	 * @return A new map projection with additional content.
	 */
	public MapProjection and(Object... content) {
		return new MapProjection(this.name, this.map.addEntries(createNewContent(content)));
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		this.name.accept(visitor);
		this.map.accept(visitor);
		visitor.leave(this);
	}

	private static List<Expression> createNewContent(Object... content) {
		final List<Expression> newContent = new ArrayList<>(content.length);
		final Set<String> knownKeys = new HashSet<>();

		String lastKey = null;
		Expression lastExpression = null;
		int i = 0;
		while (i < content.length) {

			Object next;
			if (i + 1 >= content.length) {
				next = null;
			} else {
				next = nameOrExpression(content[i + 1]);
			}
			Object current = nameOrExpression(content[i]);

			if (current instanceof String) {
				if (next instanceof Expression) {
					lastKey = (String) current;
					lastExpression = (Expression) next;
					i += 2;
				} else {
					lastKey = null;
					lastExpression = new PropertyLookup((String) current);
					i += 1;
				}
			} else if (current instanceof Expression) {
				lastKey = null;
				lastExpression = ((Expression) current);
				i += 1;
			}

			if (lastExpression instanceof Asterisk) {
				lastExpression = new PropertyLookup("*");
			}

			final Expression entry;
			if (lastKey != null) {
				Assert.isTrue(!knownKeys.contains(lastKey), "Duplicate key '" + lastKey + "'");
				entry = new KeyValueMapEntry(lastKey, lastExpression);
				knownKeys.add(lastKey);
			} else if (lastExpression instanceof SymbolicName || lastExpression instanceof PropertyLookup) {
				entry = lastExpression;
			} else if (lastExpression instanceof Property) {
				entry = ((Property) lastExpression).getName();
			} else if (lastExpression instanceof AliasedExpression) {
				AliasedExpression aliasedExpression = (AliasedExpression) lastExpression;
				entry = new KeyValueMapEntry(aliasedExpression.getAlias(), aliasedExpression.getDelegate());
			} else {
				throw new IllegalArgumentException(lastExpression + " of type " + lastExpression.getClass()
					+ " cannot be used with an implicit name as map entry.");
			}

			newContent.add(entry);
			lastKey = null;
			lastExpression = null;
		}
		return newContent;
	}
}
