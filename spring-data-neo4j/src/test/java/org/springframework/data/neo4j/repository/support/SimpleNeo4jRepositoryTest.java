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
package org.springframework.data.neo4j.repository.support;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.IdDescription;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.NodeDescription;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class SimpleNeo4jRepositoryTest {

	private NodeManager nodeManager;

	private Neo4jMappingContext mappingContext;

	private NodeDescription nodeDescription;

	private SimpleNeo4jRepository<TestNode, Long> repository;

	@BeforeEach
	void setupMock() {
		nodeManager = mock(NodeManager.class);

		nodeDescription = mock(NodeDescription.class);
		when(nodeDescription.getPrimaryLabel()).thenReturn("TestNode");
		when(nodeDescription.getIdDescription()).thenReturn(new IdDescription());

		mappingContext = mock(Neo4jMappingContext.class);
		when(mappingContext.getRequiredNodeDescription(TestNode.class)).thenReturn(nodeDescription);
		repository = new SimpleNeo4jRepository(this.nodeManager, this.mappingContext, TestNode.class);
	}

	@Test
	void saveNotImplemented() {
		repository.save(null); // todo this should throw an exception upfront
		verify(nodeManager).save(any());
	}

	@Test
	void saveAllNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.saveAll(null));
	}

	@Test
	void deleteByIdNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.deleteById(null));
	}

	@Test
	void deleteNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.delete(null));
	}

	@Test
	void deleteAll1NotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.deleteAll(Collections.emptyList()));
	}

	@Node
	class TestNode {
		@Id
		private Long id;

		private String value;
	}
}
