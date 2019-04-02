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
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.NodeManager;

/**
 * @author Gerrit Meier
 **/
class SimpleNeo4jRepositoryTest {

	private NodeManager nodeManager;

	private SimpleNeo4jRepository repository;

	@BeforeEach
	void setupMock() {
		nodeManager = mock(NodeManager.class);
		repository = new SimpleNeo4jRepository(this.nodeManager);
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
	void findByIdNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findById(null));
	}

	@Test
	void existsByIdNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.existsById(null));
	}

	@Test
	void findAllByIdNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAllById(null));
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
	void deleteAllNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.deleteAll());
	}

	@Test
	void deleteAll1NotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.deleteAll(Collections.emptyList()));
	}

	@Test
	void findOneNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findOne(null));
	}

	@Test
	void findAllNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll());
	}

	@Test
	void findAllExampleNotImplemented() {
		Example example = Example.of("");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll(example));
	}

	@Test
	void findAllPageableNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.findAll(Pageable.unpaged()));
	}

	@Test
	void findAllSortByNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.findAll(Sort.by("property")));
	}

	@Test
	void findAllExampleAndSortNotImplemented() {
		Example example = Example.of("");
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.findAll(example, Sort.by("property")));
	}

	@Test
	void findAllExampleAndPageableNotImplemented() {
		Example example = Example.of("");
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.findAll(example, Pageable.unpaged()));
	}

	@Test
	void countNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.count());
	}

	@Test
	void countWithExampleNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.count(null));
	}

	@Test
	void existsNotImplemented() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll());
	}
}
