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

import org.springframework.data.neo4j.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * A concrete condition representing a comparision between two expressions.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
public class Comparison implements Condition {

	static Comparison create(Expression lhs, String comparator, Expression rhs) {

		Assert.notNull(lhs, "Left expression must not be null.");
		Assert.hasText(comparator, "Comparator must not be empty.");
		Assert.notNull(rhs, "Right expression must not be null.");

		return new Comparison(lhs, comparator, rhs);
	}

	private final Expression left;
	private final String comparator;
	private final Expression right;

	private Comparison(Expression left, String comparator, Expression right) {

		this.left = left;
		this.comparator = comparator;
		this.right = right;
	}

	public String getComparator() {
		return comparator;
	}

	@Override
	public void accept(Visitor visitor) {

		left.accept(visitor);

		visitor.enter(this);
		visitor.leave(this);

		right.accept(visitor);
	}
}

