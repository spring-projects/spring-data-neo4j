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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class CypherTest {

	@Test
	void shouldNotCreateIllegalLiterals() {
		assertThatIllegalArgumentException().isThrownBy(() -> Cypher.literalOf(new CypherTest()))
			.withMessageStartingWith("Unsupported literal type: ");
	}

	@Test
	void shouldCreateListLiterals() {

		List<Literal<?>> params = new ArrayList<>();
		params.add(Cypher.literalFalse());
		params.add(Cypher.literalTrue());

		Literal listLiteral = Cypher.literalOf(params);

		assertThat(listLiteral).isInstanceOf(ListLiteral.class)
			.returns("[false, true]", v -> v.asString());
	}

	@Test
	void shouldCreatePropertyPointingToSymbolicName() {
		Property property = Cypher.property("a", "b");
		property.accept(segment -> {
			if (segment instanceof Property) {
				assertThat(segment).extracting(s -> ((Property) s).getName().getPropertyKeyName()).isEqualTo("b");
			} else if (segment instanceof PropertyLookup) {
				assertThat(segment).extracting(s -> ((PropertyLookup) s).getPropertyKeyName()).isEqualTo("b");
			} else if (segment instanceof SymbolicName) {
				assertThat(segment).extracting("name").containsOnly("a");
			} else {
				fail("Unexpected segment: " + segment.getClass());
			}
		});
	}

}
