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

import org.apiguardian.api.API;

/**
 * A set of operations.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
final class Operations {

	static Operation concat(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.CONCAT, op2);
	}

	static Operation add(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.ADDITION, op2);
	}

	static Operation subtract(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.SUBTRACTION, op2);
	}

	static Operation multiply(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.MULTIPLICATION, op2);
	}

	static Operation divide(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.DIVISION, op2);
	}

	static Operation remainder(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.MODULO_DIVISION, op2);
	}

	static Operation pow(Expression op1, Expression op2) {

		return Operation.create(op1, Operator.EXPONENTIATION, op2);
	}

	/**
	 * Creates a {@code =} operation. The left hand side should resolve to a property or to something which has labels
	 * or types to modify and the right hand side should either be new properties or labels.
	 *
	 * @param target The target that should be modified
	 * @param value  The new value of the target
	 * @return A new operation.
	 */
	static Operation set(Expression target, Expression value) {

		return Operation.create(target, Operator.SET, value);
	}

	static Operation set(Node target, String... label) {

		return Operation.create(target, Operator.SET_LABEL, label);
	}

	/**
	 * Not to be instantiated.
	 */
	private Operations() {
	}
}
