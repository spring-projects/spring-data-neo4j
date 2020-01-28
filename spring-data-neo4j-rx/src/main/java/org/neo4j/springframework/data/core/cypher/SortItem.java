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

import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class SortItem implements Visitable {

	private final Expression expression;
	private final Direction direction;

	static SortItem create(Expression expression, @Nullable Direction direction) {

		return new SortItem(expression, Optional.ofNullable(direction).orElse(SortItem.Direction.UNDEFINED));
	}

	private SortItem(Expression expression, Direction direction) {
		this.expression = expression;
		this.direction = direction;
	}

	public SortItem ascending() {
		return new SortItem(this.expression, Direction.ASC);
	}

	public SortItem descending() {
		return new SortItem(this.expression, Direction.DESC);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		nameOrExpression(this.expression).accept(visitor);

		if (this.direction != Direction.UNDEFINED) {
			this.direction.accept(visitor);
		}
		visitor.leave(this);
	}

	/**
	 * Sort direction.
	 * @since 1.0
	 */
	public enum Direction implements Visitable {
		UNDEFINED(""), ASC("ASC"), DESC("DESC");

		private final String symbol;

		Direction(String symbol) {
			this.symbol = symbol;
		}

		public String getSymbol() {
			return this.symbol;
		}
	}
}
