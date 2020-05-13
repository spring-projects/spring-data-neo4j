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

/**
 * Shared interface for all conditions.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public interface Condition extends Expression {

	/**
	 * Adds a condition to this condition with an {@literal AND}.
	 *
	 * @param condition The new condition to add, must not be {@literal null}.
	 * @return A new condition.
	 */
	default Condition and(Condition condition) {
		return CompoundCondition.create(this, Operator.AND, condition);
	}

	/**
	 * Adds a condition to this condition with an {@literal OR}.
	 *
	 * @param condition The new condition to add, must not be {@literal null}.
	 * @return A new condition.
	 */
	default Condition or(Condition condition) {
		return CompoundCondition.create(this, Operator.OR, condition);
	}

	/**
	 * Adds a condition to this condition with a {@literal XOR}.
	 *
	 * @param condition The new condition to add, must not be {@literal null}.
	 * @return A new condition.
	 */
	default Condition xor(Condition condition) {
		return CompoundCondition.create(this, Operator.XOR, condition);
	}

	/**
	 * Adds a condition based on a path pattern to this condition with an {@literal AND}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/4.0/clauses/where/#query-where-patterns">Using path patterns in WHERE</a>.
	 *
	 * @param pathPattern The path pattern to add to the where clause.
	 *                    This path pattern must not be {@literal null} and must
	 *                    not introduce new variables not available in the match.
	 * @return A new condition.
	 * @since 1.0.1
	 */
	default Condition and(RelationshipPattern pathPattern) {
		return CompoundCondition.create(this, Operator.AND, new RelationshipPatternCondition(pathPattern));
	}

	/**
	 * Adds a condition based on a path pattern to this condition with an {@literal OR}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/4.0/clauses/where/#query-where-patterns">Using path patterns in WHERE</a>.
	 *
	 * @param pathPattern The path pattern to add to the where clause.
	 *                    This path pattern must not be {@literal null} and must
	 *                    not introduce new variables not available in the match.
	 * @return A new condition.
	 * @since 1.0.1
	 */
	default Condition or(RelationshipPattern pathPattern) {
		return CompoundCondition.create(this, Operator.OR, new RelationshipPatternCondition(pathPattern));
	}

	/**
	 * Adds a condition based on a path pattern to this condition with a {@literal XOR}.
	 * See <a href="https://neo4j.com/docs/cypher-manual/4.0/clauses/where/#query-where-patterns">Using path patterns in WHERE</a>.
	 *
	 * @param pathPattern The path pattern to add to the where clause.
	 *                    This path pattern must not be {@literal null} and must
	 *                    not introduce new variables not available in the match.
	 * @return A new condition.
	 * @since 1.0.1
	 */
	default Condition xor(RelationshipPattern pathPattern) {
		return CompoundCondition.create(this, Operator.XOR, new RelationshipPatternCondition(pathPattern));
	}

	default Condition not() {
		return Comparison.create(Operator.NOT, this);
	}
}
