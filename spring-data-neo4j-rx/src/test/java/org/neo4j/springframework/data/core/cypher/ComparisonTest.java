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

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
class ComparisonTest {

	@Test
	void preconditionsShouldBeAsserted() {

		Expression expression = Cypher.literalOf("Arbitrary expression");
		assertThatIllegalArgumentException().isThrownBy(() -> Comparison.create(null, Operator.EQUALITY, expression))
			.withMessage("Left expression must not be null.");
		assertThatIllegalArgumentException().isThrownBy(() -> Comparison.create(expression, Operator.EQUALITY, null))
			.withMessage("Right expression must not be null.");
		assertThatIllegalArgumentException().isThrownBy(() -> Comparison.create(expression, null, expression))
			.withMessage("Operator must not be empty.");
	}
}
