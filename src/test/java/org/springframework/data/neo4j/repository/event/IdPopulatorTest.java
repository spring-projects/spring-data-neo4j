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
package org.springframework.data.neo4j.repository.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.schema.Node;

@ExtendWith(MockitoExtension.class)
class IdPopulatorTest {

	@Mock private Neo4jMappingContext neo4jMappingContext;

	@Mock private Neo4jPersistentEntity<?> nodeDescription;

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

		@Id @GeneratedValue(DummyIdGenerator.class) private String theId;
	}

	static class DummyIdGenerator implements IdGenerator<String> {

		@Override
		public String generateId(String primaryLabel, Object entity) {
			return "Not necessary unique.";
		}
	}
}
