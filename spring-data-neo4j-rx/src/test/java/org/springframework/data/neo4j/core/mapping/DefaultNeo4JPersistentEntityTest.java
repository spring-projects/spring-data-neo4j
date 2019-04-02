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

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
public class DefaultNeo4JPersistentEntityTest {

	private Neo4jMappingContext mappingContext = new Neo4jMappingContext();

	@Test
	void shouldDiscoverAnnotatedPrimaryLabel() {

		Neo4jPersistentEntity<?> entity = mappingContext.getPersistentEntity(DummySubEntity.class);

		assertThat(entity).isNotNull();
	}

	@Node("DummySubNode")
	static class DummySubEntity {
	}
}
