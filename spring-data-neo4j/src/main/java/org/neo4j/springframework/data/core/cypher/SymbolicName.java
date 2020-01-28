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

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * A symbolic name to identify nodes and relationships.
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
@API(status = API.Status.INTERNAL, since = "1.0")
public final class SymbolicName implements Expression {

	private final String name;

	static SymbolicName create(String name) {

		Assert.isTrue(Cypher.isIdentifier(name), "Name must be a valid identifier.");
		return new SymbolicName(name);
	}

	private SymbolicName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "SymbolicName{" +
			"name='" + name + '\'' +
			'}';
	}
}
