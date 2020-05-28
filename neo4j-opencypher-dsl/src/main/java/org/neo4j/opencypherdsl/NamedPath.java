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

import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * Represents a named named path as in {@code p := (a)-->(b)}.
 *
 * @author Michael J. Simons
 * @soundtrack Freddie Mercury - Never Boring
 * @since 1.1
 */
@API(status = EXPERIMENTAL, since = "1.1")
public final class NamedPath implements PatternElement, Named {

	/**
	 * The name of this path expression.
	 */
	private final SymbolicName name;

	/**
	 * The pattern defining this path.
	 */
	private final RelationshipPattern pattern;

	static OngoingDefinitionWithName named(String name) {

		return named(SymbolicName.create(name));
	}

	static OngoingDefinitionWithName named(SymbolicName name) {

		Assert.notNull(name, "A name is required");
		return new Builder(name);
	}

	/**
	 * Partial path that has a name ({@code p = }).
	 */
	public interface OngoingDefinitionWithName {

		NamedPath definedBy(RelationshipPattern pattern);
	}

	private static class Builder implements OngoingDefinitionWithName {

		private final SymbolicName name;

		private Builder(SymbolicName name) {
			this.name = name;
		}

		@Override
		public NamedPath definedBy(RelationshipPattern pattern) {
			return new NamedPath(name, pattern);
		}
	}

	private NamedPath(SymbolicName name, RelationshipPattern pattern) {
		this.name = name;
		this.pattern = pattern;
	}

	@Override
	public Optional<SymbolicName> getSymbolicName() {
		return Optional.of(name);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		this.name.accept(visitor);
		Operator.EQUALS.accept(visitor);
		this.pattern.accept(visitor);
		visitor.leave(this);
	}
}
