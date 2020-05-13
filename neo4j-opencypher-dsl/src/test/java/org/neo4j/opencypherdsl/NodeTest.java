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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
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
}
