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

import java.util.ArrayList;
import java.util.List;

import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/M14/railroad/PatternComprehension.html">PatternComprehension</a>
 * and <a href="https://neo4j.com/docs/cypher-manual/current/syntax/lists/#cypher-pattern-comprehension">the corresponding cypher manual entry</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public final class PatternComprehension implements Expression {

	private final RelationshipChain pattern;
	private @Nullable final Where where;
	private final Expression listDefinition;

	static OngoingDefinition basedOn(Relationship pattern) {

		Assert.notNull(pattern, "A pattern is required");
		return new OngoingDefinition(RelationshipChain.create(pattern));
	}

	static OngoingDefinition basedOn(RelationshipChain pattern) {

		Assert.notNull(pattern, "A pattern is required");
		return new OngoingDefinition(pattern);
	}

	/**
	 * Provides the final step of defining a pattern comprehension.
	 */
	public interface OngoingDefinitionWithPattern {

		/**
		 * @param listDefinition Defines the elements to be returned from the pattern
		 * @return The final definition of the pattern comprehension
		 */
		PatternComprehension returning(Expression... listDefinition);
	}

	/**
	 * Ongoing definition of a pattern comprehension. Can be defined without a where-clause now.
	 */
	public static class OngoingDefinition implements OngoingDefinitionWithPattern {
		private final RelationshipChain pattern;
		private Where where;

		public OngoingDefinition(RelationshipChain pattern) {
			this.pattern = pattern;
		}

		public OngoingDefinitionWithPattern where(Condition condition) {
			this.where = new Where(condition);
			return this;
		}

		@Override
		public PatternComprehension returning(Expression... expressions) {

			Assert.notNull(expressions, "Expressions are required.");
			Assert.notEmpty(expressions, "At least one expression is required.");

			List<Expression> expressionList = new ArrayList<>();
			for (Expression expression : expressions) {
				Expression newExpression = expression;
				if (expression instanceof Named) {
					newExpression = ((Named) expression)
						.getSymbolicName()
						.orElseThrow(() -> new IllegalArgumentException(
							"A named expression must have a symbolic name inside a list definition."));
				}
				expressionList.add(newExpression);
			}

			Expression listDefinition;
			if (expressions.length == 1) {
				listDefinition = expressions[0];
			} else {
				listDefinition = ListExpression.create(expressions);
			}
			return new PatternComprehension(pattern, where, listDefinition);
		}
	}

	private PatternComprehension(RelationshipChain pattern, @Nullable Where where, Expression listDefinition) {
		this.pattern = pattern;
		this.where = where;
		this.listDefinition = listDefinition;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		this.pattern.accept(visitor);
		Visitable.visitIfNotNull(this.where, visitor);
		Pipe.INSTANCE.accept(visitor);
		this.listDefinition.accept(visitor);
		visitor.leave(this);
	}
}
