/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Gerrit Meier
 */
class Neo4jRepositoryFactorySupportTest {

	@Nested
	class IdentifierTypeCheck {

		@Test
		void mismatchingClassTypes() {

			assertThatIllegalArgumentException()
				.isThrownBy(() -> Neo4jRepositoryFactorySupport.assertIdentifierType(String.class, Long.class))
				.withMessage("The repository id type class java.lang.String differs from the entity id type class java.lang.Long.");
		}

		@Test
		void mismatchingPrimitiveTypes() {

			assertThatIllegalArgumentException()
				.isThrownBy(() -> Neo4jRepositoryFactorySupport.assertIdentifierType(int.class, long.class))
				.withMessage("The repository id type int differs from the entity id type long.");
		}

		@Test
		void mismatchingPrimitiveAndClassTypes() {

			assertThatIllegalArgumentException()
				.isThrownBy(() -> Neo4jRepositoryFactorySupport.assertIdentifierType(Integer.class, long.class))
				.withMessage("The repository id type class java.lang.Integer differs from the entity id type long.");
		}

		@Test
		void matchingPrimitiveLongTypes() {
			try {
				Neo4jRepositoryFactorySupport.assertIdentifierType(long.class, long.class);
			} catch (Exception e) {
				fail("no exception should get thrown.");
			}
		}

		@Test
		void matchingPrimitiveIntTypes() {
			try {
				Neo4jRepositoryFactorySupport.assertIdentifierType(int.class, int.class);
			} catch (Exception e) {
				fail("no exception should get thrown.");
			}
		}

		@Test
		void matchingPrimitiveIntAndIntegerClassTypes() {
			try {
				Neo4jRepositoryFactorySupport.assertIdentifierType(int.class, Integer.class);
			} catch (Exception e) {
				fail("no exception should get thrown.");
			}
		}

		@Test
		void matchingIntegerClassAndPrimitiveIntTypes() {
			try {
				Neo4jRepositoryFactorySupport.assertIdentifierType(Integer.class, int.class);
			} catch (Exception e) {
				fail("no exception should get thrown.");
			}

		}

		@Test
		void matchingPrimitiveLongAndLongClassTypes() {
			try {
				Neo4jRepositoryFactorySupport.assertIdentifierType(long.class, Long.class);
			} catch (Exception e) {
				fail("no exception should get thrown.");
			}
		}

		@Test
		void matchingLongClassAndPrimitiveLongTypes() {
			try {
				Neo4jRepositoryFactorySupport.assertIdentifierType(Long.class, long.class);
			} catch (Exception e) {
				fail("no exception should get thrown.");
			}
		}

	}
}
