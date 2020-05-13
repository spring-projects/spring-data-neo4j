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
import static org.neo4j.opencypherdsl.Expressions.*;
import static org.neo4j.opencypherdsl.ListExpression.*;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitable;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/PatternComprehension.html">PatternComprehension</a>
 * and <a href="https://neo4j.com/docs/cypher-manual/current/syntax/lists/#cypher-pattern-comprehension">the corresponding cypher manual entry</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class PatternComprehension implements Expression {

	private final RelationshipPattern pattern;
	private final Where where;
	private final Expression listDefinition;

	static OngoingDefinitionWithPattern basedOn(Relationship pattern) {

		Assert.notNull(pattern, "A pattern is required");
		return new Builder(pattern);
	}

	static OngoingDefinitionWithPattern basedOn(RelationshipChain pattern) {

		Assert.notNull(pattern, "A pattern is required");
		return new Builder(pattern);
	}

	/**
	 * Allows to add a where clause into the definition of the pattern.
	 */
	public interface OngoingDefinitionWithPattern extends OngoingDefinitionWithoutReturn {

		OngoingDefinitionWithoutReturn where(Condition condition);
	}

	/**
	 * Provides the final step of defining a pattern comprehension.
	 */
	public interface OngoingDefinitionWithoutReturn {

		/**
		 * @param variables the elements to be returned from the pattern
		 * @return The final definition of the pattern comprehension
		 * @see #returning(Expression...)
		 */
		default PatternComprehension returning(Named... variables) {
			return returning(createSymbolicNames(variables));
		}

		/**
		 * @param listDefinition Defines the elements to be returned from the pattern
		 * @return The final definition of the pattern comprehension
		 */
		PatternComprehension returning(Expression... listDefinition);
	}

	/**
	 * Ongoing definition of a pattern comprehension. Can be defined without a where-clause now.
	 */
	private static class Builder implements OngoingDefinitionWithPattern {
		private final RelationshipPattern pattern;
		private Where where;

		private Builder(RelationshipPattern pattern) {
			this.pattern = pattern;
		}

		public OngoingDefinitionWithoutReturn where(Condition condition) {
			this.where = new Where(condition);
			return this;
		}

		@Override
		public PatternComprehension returning(Expression... expressions) {

			return new PatternComprehension(pattern, where, listOrSingleExpression(expressions));
		}
	}

	private PatternComprehension(RelationshipPattern pattern, Where where, Expression listDefinition) {
		this.pattern = pattern;
		this.where = where;
		this.listDefinition = listDefinition;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		this.pattern.accept(visitor);
		Visitable.visitIfNotNull(this.where, visitor);
		Operator.PIPE.accept(visitor);
		this.listDefinition.accept(visitor);
		visitor.leave(this);
	}
}
