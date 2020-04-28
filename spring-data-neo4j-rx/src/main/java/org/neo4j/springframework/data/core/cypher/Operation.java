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

import static java.util.stream.Collectors.*;
import static org.apiguardian.api.API.Status.*;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * A binary operation.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Operation implements Expression {

	/**
	 * A set of operators triggering operations on labels.
	 */
	private static final EnumSet<Operator> LABEL_OPERATORS = EnumSet.of(Operator.SET_LABEL);
	private static final EnumSet<Operator.Type> NEEDS_GROUPING_BY_TYPE = EnumSet
		.complementOf(EnumSet.of(Operator.Type.PROPERTY, Operator.Type.LABEL));
	private static final EnumSet<Operator> DONT_GROUP = EnumSet
		.complementOf(EnumSet.of(Operator.EXPONENTIATION, Operator.PIPE));

	static Operation create(Expression op1, Operator operator, Expression op2) {

		Assert.notNull(op1, "The first operand must not be null.");
		Assert.notNull(operator, "Operator must not be empty.");
		Assert.notNull(op2, "The second operand must not be null.");

		return new Operation(op1, operator, op2);
	}

	static Operation create(Node op1, Operator operator, String... nodeLabels) {

		Assert.notNull(op1, "The first operand must not be null.");
		Assert.isTrue(op1.getSymbolicName().isPresent(), "The node must have a name.");
		Assert.isTrue(LABEL_OPERATORS.contains(operator),
			() -> String.format("Only operators %s can be used to modify labels",
				LABEL_OPERATORS));
		Assert.notEmpty(nodeLabels, "The labels cannot be empty.");

		List<NodeLabel> listOfNodeLabels = Arrays.stream(nodeLabels).map(NodeLabel::new).collect(toList());
		return new Operation(op1.getRequiredSymbolicName(), operator, new NodeLabels(listOfNodeLabels));
	}

	private final Expression left;
	private final Operator operator;
	private final Visitable right;

	Operation(Expression left, Operator operator, Expression right) {

		this.left = left;
		this.operator = operator;
		this.right = right;
	}

	Operation(Expression left, Operator operator, NodeLabels right) {

		this.left = left;
		this.operator = operator;
		this.right = right;
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		Expressions.nameOrExpression(left).accept(visitor);
		operator.accept(visitor);
		right.accept(visitor);
		visitor.leave(this);
	}

	/**
	 * Checks, whether this operation needs grouping.
	 *
	 * @return True, if this operation needs grouping.
	 */
	public boolean needsGrouping() {
		return NEEDS_GROUPING_BY_TYPE.contains(this.operator.getType()) && this.operator != Operator.EXPONENTIATION;
	}
}
