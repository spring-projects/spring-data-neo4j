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

import static org.neo4j.springframework.data.core.cypher.Operator.*;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A condition that consists of one or two {@link Condition conditions} connected by a
 * <a href="https://en.wikipedia.org/wiki/Logical_connective">Logical connective (operator)</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class CompoundCondition implements Condition {

	/**
	 * The empty, compound condition.
	 */
	static final CompoundCondition EMPTY_CONDITION = new CompoundCondition(null);
	static final EnumSet<Operator> VALID_OPERATORS = EnumSet.of(AND, OR, XOR);

	static CompoundCondition create(Condition left, Operator operator, Condition right) {

		Assert.isTrue(VALID_OPERATORS.contains(operator),
			"Operator " + operator + " is not a valid operator for a compound condition.");

		Assert.notNull(left, "Left hand side condition is required.");
		Assert.notNull(operator, "Operator is required.");
		Assert.notNull(right, "Right hand side condition is required.");

		return new CompoundCondition(operator)
			.add(operator, left)
			.add(operator, right);
	}

	static CompoundCondition empty() {

		return EMPTY_CONDITION;
	}

	private @Nullable final Operator operator;

	private final List<Condition> conditions;

	private CompoundCondition(@Nullable Operator operator) {
		this.operator = operator;
		this.conditions = new ArrayList<>();
	}

	@Override
	public Condition and(Condition condition) {
		return this.add(AND, condition);
	}

	@Override
	public Condition or(Condition condition) {
		return this.add(OR, condition);
	}

	@Override
	public Condition xor(Condition condition) {
		return this.add(XOR, condition);
	}

	private CompoundCondition add(
		Operator chainingOperator,
		Condition condition
	) {
		if (this == EMPTY_CONDITION) {
			return new CompoundCondition(chainingOperator).add(chainingOperator, condition);
		}

		if (condition == EMPTY_CONDITION) {
			return this;
		}

		if (condition instanceof CompoundCondition) {
			CompoundCondition compoundCondition = (CompoundCondition) condition;
			if (compoundCondition.operator == chainingOperator) {
				this.conditions.addAll(compoundCondition.conditions);
			} else {
				this.conditions.add(condition);
			}

			return this;
		}

		if (this.operator == chainingOperator) {
			conditions.add(condition);
			return this;
		}

		return CompoundCondition.create(this, chainingOperator, condition);
	}

	@Override
	public void accept(Visitor visitor) {

		// Fold single or empty condition
		boolean hasManyConditions = this.conditions.size() > 1;

		if (hasManyConditions) {
			visitor.enter(this);
		}

		Operator currentOperator = null;
		for (Condition condition : conditions) {
			Visitable.visitIfNotNull(currentOperator, visitor);
			condition.accept(visitor);
			currentOperator = operator;
		}

		if (hasManyConditions) {
			visitor.leave(this);
		}
	}
}
