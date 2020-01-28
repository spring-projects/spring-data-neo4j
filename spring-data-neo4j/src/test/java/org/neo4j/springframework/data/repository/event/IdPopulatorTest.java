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
package org.neo4j.springframework.data.repository.event;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.IdDescription;
import org.neo4j.springframework.data.core.schema.IdGenerator;
import org.neo4j.springframework.data.core.schema.Node;

@ExtendWith(MockitoExtension.class)
class IdPopulatorTest {

	@Mock
	private Neo4jMappingContext neo4jMappingContext;

	@Mock
	private Neo4jPersistentEntity<?> nodeDescription;

	@Test
	void shouldRejectNullMappingContext() {
		Assertions.assertThatIllegalArgumentException().isThrownBy(() -> new IdPopulator(null))
			.withMessage("A mapping context is required.");
	}

	@Test
	void shouldRejectNullEntity() {
		IdPopulator idPopulator = new IdPopulator(neo4jMappingContext);
		Assertions.assertThatIllegalArgumentException().isThrownBy(() -> idPopulator.populateIfNecessary(null))
			.withMessage("Entity may not be null!");
	}

	@Test
	void shouldIgnoreInternalIdGenerator() {

		IdDescription toBeReturned = IdDescription.forInternallyGeneratedIds();
		doReturn(toBeReturned).when(nodeDescription).getIdDescription();
		doReturn(nodeDescription).when(neo4jMappingContext).getRequiredPersistentEntity(Sample.class);

		IdPopulator idPopulator = new IdPopulator(neo4jMappingContext);
		Sample sample = new Sample();

		assertThat(idPopulator.populateIfNecessary(sample)).isSameAs(sample);

		verify(nodeDescription).getIdDescription();
		verify(neo4jMappingContext).getRequiredPersistentEntity(Sample.class);

		verifyNoMoreInteractions(nodeDescription, neo4jMappingContext);
	}

	@Test
	void shouldNotActOnAssignedProperty() {

		IdPopulator idPopulator = new IdPopulator(new Neo4jMappingContext());
		Sample sample = new Sample();
		sample.theId = "something";

		Sample populatedSample = (Sample) idPopulator.populateIfNecessary(sample);
		assertThat(populatedSample).isSameAs(sample);
		assertThat(populatedSample.theId).isEqualTo("something");
	}

	@Test
	void shouldInvokeGenerator() {

		IdPopulator idPopulator = new IdPopulator(new Neo4jMappingContext());
		Sample sample = new Sample();

		Sample populatedSample = (Sample) idPopulator.populateIfNecessary(sample);
		assertThat(populatedSample).isSameAs(sample);
		assertThat(populatedSample.theId).isEqualTo("Not necessary unique.");
	}

	@Node
	static class Sample {

		@Id @GeneratedValue(DummyIdGenerator.class)
		private String theId;
	}

	static class DummyIdGenerator implements IdGenerator<String> {

		@Override
		public String generateId(String primaryLabel, Object entity) {
			return "Not necessary unique.";
		}
	}
}
