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
import static org.junit.jupiter.api.TestInstance.Lifecycle.*;
import static org.neo4j.opencypherdsl.Cypher.*;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.opencypherdsl.support.Visitable;
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * @author Michael J. Simons
 */
class RelationshipTest {

	@Test
	void preconditionsShouldBeAsserted() {

		assertThatIllegalArgumentException()
			.isThrownBy(() -> Relationship.create(null, null, Node.create("a"), new String[0]))
			.withMessage("Left node is required.");
		assertThatIllegalArgumentException()
			.isThrownBy(() -> Relationship.create(Node.create("a"), null, null, new String[0]))
			.withMessage("Right node is required.");
	}

	@Nested
	@TestInstance(PER_CLASS)
	class PropertiesShouldBeHandled {

		private Stream<Arguments> createNodesWithProperties() {
			return Stream.of(
				Arguments.of(Node.create("N").named("n").relationshipTo(Cypher.anyNode())
					.withProperties("p", literalTrue())),
				Arguments.of(Node.create("N").named("n").relationshipTo(Cypher.anyNode())
					.withProperties(MapExpression.create("p", literalTrue())))
			);
		}

		@ParameterizedTest
		@MethodSource("createNodesWithProperties")
		void shouldAddProperties(Relationship relationship) {

			AtomicBoolean failTest = new AtomicBoolean(true);
			relationship.accept(new Visitor() {
				Class<?> expectedTypeOfNextSegment = null;

				@Override
				public void enter(Visitable segment) {
					if (segment instanceof SymbolicName) {
						assertThat(((SymbolicName) segment).getValue()).isEqualTo("n");
					} else if (segment instanceof NodeLabel) {
						assertThat(((NodeLabel) segment).getValue()).isEqualTo("N");
					} else if (segment instanceof KeyValueMapEntry) {
						assertThat(((KeyValueMapEntry) segment).getKey()).isEqualTo("p");
						expectedTypeOfNextSegment = BooleanLiteral.class;
					} else if (expectedTypeOfNextSegment != null) {
						assertThat(segment).isInstanceOf(expectedTypeOfNextSegment);
						failTest.getAndSet(false);
					}
				}

				@Override
				public void leave(Visitable segment) {
					if (expectedTypeOfNextSegment == BooleanLiteral.class) {
						failTest.getAndSet(true);
						expectedTypeOfNextSegment = Node.class;
					}
				}
			});
			assertThat(failTest).isFalse();
		}

		@Test
		void shouldCreateProperty() {

			Relationship relationship = Node.create("N").named("n").relationshipTo(Cypher.anyNode()).named("r");
			Property property = relationship.property("p");

			java.util.Set<Object> expected = new HashSet<>();
			expected.add(property.getName());
			expected.add(relationship.getRequiredSymbolicName());
			expected.add(property);

			property.accept(expected::remove);

			assertThat(expected).isEmpty();
		}
	}
}
