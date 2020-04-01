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

import java.util.Objects;

import org.apiguardian.api.API;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * A symbolic name to identify nodes, relationships and aliased items.
 * <p>
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/SchemaName.html">SchemaName</a>
 * <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/SymbolicName.html">SymbolicName</a>
 * <p>
 * While OpenCypher extends the <a href="https://unicode.org/reports/tr31/">UNICODE IDENTIFIER AND PATTERN SYNTAX</a>
 * with some characters, this DSL uses the same identifier Java itself uses for simplicity and until otherwise needed.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class SymbolicName implements Expression {

	private final String value;

	static SymbolicName create(String name) {

		Assert.hasText(name, "Name must not be empty.");
		Assert.isTrue(Cypher.isIdentifier(name), "Name must be a valid identifier.");
		return new SymbolicName(name);
	}

	private SymbolicName(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@NonNull
	public SymbolicName concat(String otherValue) {

		Assert.notNull(otherValue, "Value to concat must not be null.");
		if (otherValue.isEmpty()) {
			return this;
		}
		return SymbolicName.create(this.value + otherValue);
	}

	@Override
	public String toString() {
		return "SymbolicName{" +
			"name='" + value + '\'' +
			'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SymbolicName that = (SymbolicName) o;
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
