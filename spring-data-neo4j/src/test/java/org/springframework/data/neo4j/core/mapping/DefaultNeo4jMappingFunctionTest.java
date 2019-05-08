/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PreferredConstructor;

/**
 * @author Michael J. Simons
 */
class DefaultNeo4jMappingFunctionTest {

	@Nested
	class PropertyNames {

		@Test
		void shouldDefaultToParameterNames() {
			Neo4jPersistentEntity<?> nodeDescription = mock(Neo4jPersistentEntity.class);
			when(nodeDescription.getGraphProperty(Mockito.eq("aParameter"))).thenReturn(Optional.empty());

			PreferredConstructor.Parameter parameter = mock(PreferredConstructor.Parameter.class);
			when(parameter.getName()).thenReturn("aParameter");

			DefaultNeo4jMappingFunction mappingFunction = new DefaultNeo4jMappingFunction(
				mock(EntityInstantiators.class), nodeDescription);

			assertThat(mappingFunction.getGraphPropertyNameFor(parameter)).isEqualTo("aParameter");
		}
	}
}
