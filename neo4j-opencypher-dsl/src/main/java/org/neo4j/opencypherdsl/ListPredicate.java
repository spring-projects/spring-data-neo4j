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

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * A list predicate.
 *
 * @author Michael J. Simons
 * @soundtrack Freddie Mercury - Never Boring
 * @since 1.1
 * /
 */
@API(status = INTERNAL, since = "1.1")
final class ListPredicate implements Expression {

	/**
	 * The variable for the where part.
	 */
	private final SymbolicName variable;

	/**
	 * The list expression. No further assertions are taken to check beforehand if it is actually a Cypher List atm.
	 */
	private final Expression listExpression;

	/**
	 * Filtering on the list.
	 */
	private final Where where;

	ListPredicate(SymbolicName variable, Expression listExpression, Where where) {

		this.variable = variable;
		this.listExpression = listExpression;
		this.where = where;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		this.variable.accept(visitor);
		Operator.IN.accept(visitor);
		this.listExpression.accept(visitor);
		this.where.accept(visitor);
		visitor.leave(this);
	}
}
