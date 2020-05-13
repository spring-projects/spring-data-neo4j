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

import static org.assertj.core.api.Assertions.*;
import static org.neo4j.opencypherdsl.Cypher.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.opencypherdsl.support.Visitable;

/**
 * @author Michael J. Simons
 */
class ExpressionTest {

	@ParameterizedTest
	@MethodSource("mathematicalOperators")
	void correctMathematicalOperatorsShouldBeUsed(Operation operation, Operator expectedOperator) {

		AtomicInteger counter = new AtomicInteger(0);

		operation.accept(segment -> {
			int i = counter.getAndIncrement();
			switch (i) {
				case 0:
					assertThat(segment).isInstanceOf(Operation.class);
					break;
				case 1:
					assertThat(segment)
						.isInstanceOfSatisfying(NumberLiteral.class, v -> assertThat(v.getContent()).isEqualTo(1));
					break;
				case 2:
					assertThat(segment)
						.isInstanceOfSatisfying(Operator.class, v -> assertThat(v)).isEqualTo(expectedOperator);
					break;
				case 3:
					assertThat(segment)
						.isInstanceOfSatisfying(NumberLiteral.class, v -> assertThat(v.getContent()).isEqualTo(2));
					break;
				default:
					throw new IllegalArgumentException("Too many segments to visit.");
			}
		});
		assertThat(counter.get()).isEqualTo(4);
	}

	@ParameterizedTest
	@MethodSource("stringOperators")
	void correctStringOperatorsShouldBeUsed(Visitable visitable, Operator expectedOperator) {

		AtomicInteger counter = new AtomicInteger(0);

		visitable.accept(segment -> {
			int i = counter.getAndIncrement();
			switch (i) {
				case 0:
					break;
				case 1:
					assertThat(segment)
						.isInstanceOfSatisfying(StringLiteral.class, v -> assertThat(v.getContent()).isEqualTo("a"));
					break;
				case 2:
					assertThat(segment)
						.isInstanceOfSatisfying(Operator.class, v -> assertThat(v)).isEqualTo(expectedOperator);
					break;
				case 3:
					assertThat(segment)
						.isInstanceOfSatisfying(StringLiteral.class, v -> assertThat(v.getContent()).isEqualTo("b"));
					break;
				default:
					throw new IllegalArgumentException("Too many segments to visit.");
			}
		});
		assertThat(counter.get()).isEqualTo(4);
	}

	private static Stream<Arguments> mathematicalOperators() {
		return Stream.of(
			Arguments.of(literalOf(1).add(literalOf(2)), Operator.ADDITION),
			Arguments.of(literalOf(1).subtract(literalOf(2)), Operator.SUBTRACTION),
			Arguments.of(literalOf(1).multiply(literalOf(2)), Operator.MULTIPLICATION),
			Arguments.of(literalOf(1).divide(literalOf(2)), Operator.DIVISION),
			Arguments.of(literalOf(1).remainder(literalOf(2)), Operator.MODULO_DIVISION),
			Arguments.of(literalOf(1).pow(literalOf(2)), Operator.EXPONENTIATION)
		);
	}

	private static Stream<Arguments> stringOperators() {
		return Stream.of(
			Arguments.of(literalOf("a").concat(literalOf("b")), Operator.CONCAT),
			Arguments.of(literalOf("a").matches(literalOf("b")), Operator.MATCHES)
		);
	}
}
