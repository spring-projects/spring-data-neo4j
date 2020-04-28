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
import static org.neo4j.springframework.data.core.cypher.Expressions.*;
import static org.neo4j.springframework.data.core.cypher.ListExpression.*;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/Atom.html#ListComprehension">ListComprehension</a>
 * and <a href="https://neo4j.com/docs/cypher-manual/current/syntax/lists/#cypher-list-comprehension">the corresponding cypher manual entry</a>.
 *
 * @author Michael J. Simons
 * @soundtrack Corrosion Of Conformity - America's Volume Dealer
 * @since 1.0.1
 */
@API(status = EXPERIMENTAL, since = "1.0.1")
public final class ListComprehension implements Expression {

	// Modelling from the FilterExpression: https://s3.amazonaws.com/artifacts.opencypher.org/M14/railroad/FilterExpression.html */

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
	private @Nullable final Where where;

	/**
	 * The new list to be returned.
	 */
	private @Nullable final Expression listDefinition;

	static OngoingDefinitionWithVariable with(SymbolicName variable) {

		Assert.notNull(variable, "A variable is required");
		return new Builder(variable);
	}

	/**
	 * {@link #in(Expression)} must be used to define the source list.
	 */
	public interface OngoingDefinitionWithVariable {

		OngoingDefinitionWithList in(Expression list);
	}

	/**
	 * Allows to add a where clause into the definition of the list.
	 */
	public interface OngoingDefinitionWithList extends OngoingDefinitionWithoutReturn {

		OngoingDefinitionWithoutReturn where(Condition condition);
	}

	/**
	 * Provides the final step of defining a list comprehension.
	 */
	public interface OngoingDefinitionWithoutReturn {

		/**
		 * @param variables the elements to be returned from the list
		 * @return The final definition of the list comprehension
		 * @see #returning(Expression...)
		 */
		default ListComprehension returning(Named... variables) {
			return returning(createSymbolicNames(variables));
		}

		/**
		 * @param listDefinition Defines the elements to be returned from the pattern
		 * @return The final definition of the list comprehension
		 */
		ListComprehension returning(Expression... listDefinition);

		/**
		 * @return Returns the list comprehension as is, without a {@literal WHERE} and returning each element of the
		 * original list
		 */
		ListComprehension returning();
	}

	private static class Builder
		implements OngoingDefinitionWithVariable, OngoingDefinitionWithList {

		private final SymbolicName variable;
		private Expression listExpression;
		private Where where;

		private Builder(SymbolicName variable) {
			this.variable = variable;
		}

		@Override
		public OngoingDefinitionWithList in(Expression list) {
			this.listExpression = list;
			return this;
		}

		@Override
		public OngoingDefinitionWithoutReturn where(Condition condition) {
			this.where = new Where(condition);
			return this;
		}

		@Override
		public ListComprehension returning() {

			return new ListComprehension(variable, listExpression, where, null);
		}

		@Override
		public ListComprehension returning(Expression... expressions) {

			return new ListComprehension(variable, listExpression, where, listOrSingleExpression(expressions));
		}
	}

	private ListComprehension(SymbolicName variable, Expression listExpression,
		@Nullable Where where, @Nullable Expression listDefinition) {
		this.variable = variable;
		this.listExpression = listExpression;
		this.where = where;
		this.listDefinition = listDefinition;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		this.variable.accept(visitor);
		Operator.IN.accept(visitor);
		this.listExpression.accept(visitor);
		Visitable.visitIfNotNull(this.where, visitor);
		if (this.listDefinition != null) {
			Operator.PIPE.accept(visitor);
			this.listDefinition.accept(visitor);
		}
		visitor.leave(this);
	}
}
