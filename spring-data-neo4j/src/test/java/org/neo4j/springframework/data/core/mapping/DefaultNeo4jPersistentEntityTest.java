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
package org.neo4j.springframework.data.core.mapping;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Property;

/**
 * @author Gerrit Meier
 */
class DefaultNeo4jPersistentEntityTest {

	@Test
	void persistentEntityCreationWorksForCorrectEntity() {
		new Neo4jMappingContext().getPersistentEntity(CorrectEntity.class);

	}

	@Test
	void failsOnDuplicatedEntityProperties() {
		assertThatIllegalStateException()
				.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(EntityWithDuplicatedProperties.class))
				.withMessage("Duplicate definition of property [name] in entity class "
						+ "org.neo4j.springframework.data.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithDuplicatedProperties.");
	}

	@Test
	void failsOnMultipleDuplicatedEntityProperties() {
		assertThatIllegalStateException()
			.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(EntityWithMultipleDuplicatedProperties.class))
			.withMessage("Duplicate definition of properties [foo, name] in entity class "
				+ "org.neo4j.springframework.data.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithMultipleDuplicatedProperties.");
	}

	@Node
	private static class CorrectEntity {

		@Id private Long id;

		private String name;
	}

	@Node
	private static class EntityWithDuplicatedProperties {

		@Id private Long id;

		private String name;

		@Property("name") private String alsoName;
	}

	@Node
	private static class EntityWithMultipleDuplicatedProperties {

		@Id private Long id;

		private String name;

		@Property("name") private String alsoName;

		@Property("foo")
		private String somethingElse;

		@Property("foo")
		private String thisToo;
	}

}
