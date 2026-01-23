/*
 * Copyright 2011-present the original author or authors.
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

package org.springframework.data.neo4j.core.convert;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.Entity;
import org.neo4j.driver.types.MapAccessor;
import org.neo4j.driver.types.Node;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Neo4jSimpleTypes}.
 *
 * @author Mark Paluch
 */
class Neo4jSimpleTypesUnitTests {

	@ParameterizedTest
	@ValueSource(classes = { MapAccessor.class, Node.class, Entity.class, Value.class })
	void shouldConsiderSimpleDriverTypes(Class<?> type) {
		assertThat(Neo4jSimpleTypes.HOLDER.isSimpleType(type)).isTrue();
	}

}
