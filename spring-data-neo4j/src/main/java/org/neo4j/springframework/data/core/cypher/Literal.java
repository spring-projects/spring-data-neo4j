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

import org.apiguardian.api.API;
import org.springframework.lang.Nullable;

/**
 * Represents a literal with an optional content.
 *
 * @author Michael J. Simons
 * @param <T> type of content
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public abstract class Literal<T> implements Expression {

	/**
	 * The content of this literal.
	 */
	private @Nullable T content;

	Literal(@Nullable T content) {
		this.content = content;
	}

	/**
	 * @return The content of this literal, may be {@literal null}
	 */
	public @Nullable T getContent() {
		return content;
	}

	/**
	 * The string representation should be designed in such a way the a renderer can use it correctly in
	 * the given context of the literal, i.e. a literal containing a string should quote that string
	 * and escape all reserved characters.
	 *
	 * @return A string representation to be used literally in a cypher statement.
	 */
	public abstract String asString();
}

