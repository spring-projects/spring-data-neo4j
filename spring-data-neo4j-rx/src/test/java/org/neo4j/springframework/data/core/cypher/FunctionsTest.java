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
import static org.mockito.Mockito.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class FunctionsTest {

	private static final String FUNCTION_NAME_FIELD = "functionName";

	@Nested
	class id {

		@Test
		void preconditionsShouldBeAsserted() {

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.id((Node) null))
				.withMessage("The node parameter is required.");

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.id((Relationship) null))
				.withMessage("The relationship parameter is required.");
		}

		@Test
		void shouldCreateCorrectInvocation() {

			FunctionInvocation invocation = Functions.id(node("Test"));
			assertThat(invocation).hasFieldOrPropertyWithValue(FUNCTION_NAME_FIELD, "id");
		}
	}

	@Nested
	class count {

		@Test
		void preconditionsShouldBeAsserted() {

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.count(null))
				.withMessage("The expression to count is required.");
		}

		@Test
		void shouldCreateCorrectInvocation() {

			FunctionInvocation invocation = Functions.count(mock(Expression.class));
			assertThat(invocation).hasFieldOrPropertyWithValue(FUNCTION_NAME_FIELD, "count");
		}
	}

	@Nested
	class coalesce {

		@Test
		void preconditionsShouldBeAsserted() {

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.coalesce((Expression[]) null))
				.withMessage("At least one expression is required.");

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.coalesce(new Expression[0]))
				.withMessage("At least one expression is required.");

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.coalesce(new Expression[] { null }))
				.withMessage("At least one expression is required.");
		}

		@Test
		void shouldCreateCorrectInvocation() {

			FunctionInvocation invocation = Functions.coalesce(mock(Expression.class));
			assertThat(invocation).hasFieldOrPropertyWithValue(FUNCTION_NAME_FIELD, "coalesce");
		}
	}

	@Nested
	class collect {

		@Test
		void preconditionsShouldBeAsserted() {

			assertThatIllegalArgumentException().isThrownBy(() -> Functions.collect(null))
				.withMessage("The expression to collect is required.");
		}

		@Test
		void shouldCreateCorrectInvocation() {

			FunctionInvocation invocation = Functions.collect(mock(Expression.class));
			assertThat(invocation).hasFieldOrPropertyWithValue(FUNCTION_NAME_FIELD, "collect");
		}
	}
}
