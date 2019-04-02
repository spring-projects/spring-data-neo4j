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
package org.springframework.data.neo4j.repository.query;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

/**
 * @author Gerrit Meier
 **/
class Neo4jQueryMethodTest {

	private static final String CUSTOM_CYPHER_QUERY = "MATCH (n) return n";

	@Test
	void shouldFindAnnotatedQuery() throws Exception {
		Neo4jQueryMethod queryMethod = queryMethod("annotatedQuery");
		assertThat(queryMethod.getAnnotatedQuery()).isEqualTo(CUSTOM_CYPHER_QUERY);
	}

	@Test
	void findQueryAnnotation() throws Exception {
		Neo4jQueryMethod queryMethod = queryMethod("annotatedQuery");
		assertThat(queryMethod.hasAnnotatedQuery()).isTrue();
	}

	private Neo4jQueryMethod queryMethod(String name, Class<?>... parameters) throws Exception {
		Class<PersonRepository> repositoryClass = PersonRepository.class;

		Method method = repositoryClass.getMethod(name, parameters);
		ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
		return new Neo4jQueryMethod(method, new DefaultRepositoryMetadata(repositoryClass), factory);
	}

	interface PersonRepository extends Repository<Person, Long> {

		@Query(CUSTOM_CYPHER_QUERY)
		List<Person> annotatedQuery();
	}

	class Person {
	}

}
