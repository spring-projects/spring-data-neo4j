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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
class NodeTest {

	@Test
	void preconditionsShouldBeAsserted() {
		String expectedMessage = "A primary label is required.";

		assertThatIllegalArgumentException().isThrownBy(() -> Node.create("")).withMessage(expectedMessage);
		assertThatIllegalArgumentException().isThrownBy(() -> Node.create(" \t")).withMessage(expectedMessage);
	}

	@Test
	void shouldNotAddEmptyAdditionalLabels() {

		assertThatIllegalArgumentException().isThrownBy(() -> Node.create("primary", " ", "\t "))
			.withMessage("An empty label is not allowed.");
	}

	@Test
	void shouldCreateNodes() {

		Node node = Node.create("primary", "secondary");
		List<String> labels = new ArrayList<>();
		node.accept(new Visitor() {
			@Override
			public void enter(Visitable segment) {

				if (segment instanceof NodeLabel) {
					labels.add(((NodeLabel) segment).getValue());
				}

			}
		});
		assertThat(labels).contains("primary", "secondary");
	}

	@Nested
	@TestInstance(PER_CLASS)
	class PropertiesShouldBeHandled {

		private Stream<Arguments> createNodesWithProperties() {
			return Stream.of(
				Arguments.of(Node.create("N").named("n").withProperties("p", literalTrue())),
				Arguments.of(Node.create("N").named("n").withProperties(MapExpression.create("p", literalTrue())))
			);
		}

		@ParameterizedTest
		@MethodSource("createNodesWithProperties")
		void shouldAddProperties(Node node) {

			AtomicBoolean failTest = new AtomicBoolean(true);
			node.accept(new Visitor() {
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
			});
			assertThat(failTest).isFalse();
		}

		@Test
		void shouldCreateProperty() {

			Node node = Node.create("N").named("n");
			Property property = node.property("p");

			java.util.Set<Object> expected = new HashSet<>();
			expected.add(property.getName());
			expected.add(node.getRequiredSymbolicName());
			expected.add(property);

			property.accept(expected::remove);

			assertThat(expected).isEmpty();
		}
	}
}
