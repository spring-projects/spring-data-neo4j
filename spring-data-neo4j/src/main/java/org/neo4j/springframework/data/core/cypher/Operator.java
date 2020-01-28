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
import org.neo4j.springframework.data.core.cypher.support.Visitable;

/**
 * An operator. See <a href="https://neo4j.com/docs/cypher-manual/current/syntax/operators/#query-operators-summary">Operators</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public enum Operator implements Visitable {

	// Mathematical operators
	ADDITION("+"),
	SUBTRACTION("-"),

	// Comparison operators
	EQUALITY("="),
	INEQUALITY("<>"),
	LESS_THAN("<"),
	GREATER_THAN(">"),
	LESS_THAN_OR_EQUAL_TO("<="),
	GREATER_THAN_OR_EQUAL_TO(">="),
	IS_NULL("IS NULL", Type.POSTFIX),
	IS_NOT_NULL("IS NOT NULL", Type.POSTFIX),

	STARTS_WITH("STARTS WITH"),
	ENDS_WITH("ENDS WITH"),
	CONTAINS("CONTAINS"),

	// Boolean operators
	AND("AND"),
	OR("OR"),
	XOR("XOR"),
	NOT("NOT", Type.PREFIX),

	// String operators
	MATCHES("=~"),

	// List operators
	IN("IN"),

	// Property operators
	SET("=", Type.PROPERTY),
	GET(".", Type.PROPERTY),
	MUTATE("+=", Type.PROPERTY),

	// Node operators
	SET_LABEL("", Type.LABEL),
	REMOVE_LABEL("", Type.LABEL);

	private final String representation;

	private final Type type;

	Operator(String representation) {
		this(representation, Type.BINARY);
	}

	Operator(String representation, Type type) {
		this.representation = representation;
		this.type = type;
	}

	public String getRepresentation() {
		return representation;
	}

	public boolean isUnary() {
		return type != Type.BINARY;
	}

	public Type getType() {
		return type;
	}

	/**
	 * {@link Operator} type.
	 * @since 1.0
	 */
	public enum Type {
		BINARY,
		PREFIX,
		POSTFIX,
		PROPERTY,
		LABEL
	}
}
