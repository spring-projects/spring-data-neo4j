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
package org.springframework.data.neo4j.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.TypeSystem;

/**
 * @author Michael J. Simons
 */
class DelegatingMappingFunctionWithNullCheckTest {

	@Test
	void shouldBeHappyWithNonNullValues() {
		DelegatingMappingFunctionWithNullCheck<String> function = new DelegatingMappingFunctionWithNullCheck(
				(typeSystem, record) -> "Tada.");
		assertThat(function.apply(mock(TypeSystem.class), mock(Record.class))).isEqualTo("Tada.");
	}

	@Test
	void shouldThrowExceptions() {
		DelegatingMappingFunctionWithNullCheck<String> function = new DelegatingMappingFunctionWithNullCheck(
				(typeSystem, record) -> null);
		assertThatIllegalStateException().isThrownBy(() -> function.apply(mock(TypeSystem.class), mock(Record.class)))
				.withMessageMatching("Mapping function .* returned illegal null value for record .*");
	}
}
