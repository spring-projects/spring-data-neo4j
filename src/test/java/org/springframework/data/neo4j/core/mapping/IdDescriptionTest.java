/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.IdGenerator;

/**
 * @author Michael J. Simons
 */
class IdDescriptionTest {

	@Test
	void isAssignedShouldWork() {

		assertThat(IdDescription.forAssignedIds(Constants.NAME_OF_ROOT_NODE, "foobar").isAssignedId()).isTrue();
		assertThat(IdDescription.forAssignedIds(Constants.NAME_OF_ROOT_NODE, "foobar").isExternallyGeneratedId()).isFalse();
		assertThat(IdDescription.forAssignedIds(Constants.NAME_OF_ROOT_NODE, "foobar").isInternallyGeneratedId()).isFalse();
	}

	@Test
	void idIsGeneratedInternallyShouldWork() {

		assertThat(IdDescription.forInternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE).isAssignedId()).isFalse();
		assertThat(IdDescription.forInternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE).isExternallyGeneratedId()).isFalse();
		assertThat(IdDescription.forInternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE).isInternallyGeneratedId()).isTrue();
	}

	@Test
	void idIsGeneratedExternally() {

		assertThat(IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE, DummyIdGenerator.class, null, "foobar").isAssignedId())
				.isFalse();
		assertThat(
				IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE, DummyIdGenerator.class, null, "foobar").isExternallyGeneratedId())
						.isTrue();
		assertThat(
				IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE, DummyIdGenerator.class, null, "foobar").isInternallyGeneratedId())
						.isFalse();

		assertThat(IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE, null, "someId", "foobar").isAssignedId()).isFalse();
		assertThat(IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE, null, "someId", "foobar").isExternallyGeneratedId()).isTrue();
		assertThat(IdDescription.forExternallyGeneratedIds(Constants.NAME_OF_ROOT_NODE, null, "someId", "foobar").isInternallyGeneratedId()).isFalse();
	}

	private static class DummyIdGenerator implements IdGenerator<Void> {

		@Override
		public Void generateId(String primaryLabel, Object entity) {
			return null;
		}
	}
}
