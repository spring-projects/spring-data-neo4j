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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.Repository;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class Neo4jQueryLookupStrategyTest {

	private static final String CUSTOM_CYPHER_QUERY = "MATCH (n) return n";

	@Test
	void shouldFindAnnotatedQuery() throws Exception {

		Method method = queryMethod("annotatedQuery");
		Optional<Query> optionalQueryAnnotation = Neo4jQueryLookupStrategy.getQueryAnnotationOf(method);
		assertThat(Neo4jQueryLookupStrategy.getCypherQuery(optionalQueryAnnotation)).isEqualTo(CUSTOM_CYPHER_QUERY);
	}

	@Test
	void shouldDetectInvalidAnnotation() throws Exception {

		Method method = queryMethod("invalidAnnotatedQuery");
		Optional<Query> optionalQueryAnnotation = Neo4jQueryLookupStrategy.getQueryAnnotationOf(method);
		assertThatExceptionOfType(MappingException.class)
			.isThrownBy(() -> Neo4jQueryLookupStrategy.getCypherQuery(optionalQueryAnnotation))
			.withMessage("Expected @Query annotation to have a value, but it did not.");
	}

	@Test
	void findQueryAnnotation() throws Exception {

		Method method = queryMethod("annotatedQuery");
		Optional<Query> optionalQueryAnnotation = Neo4jQueryLookupStrategy.getQueryAnnotationOf(method);
		assertThat(optionalQueryAnnotation).isPresent();
	}

	private Method queryMethod(String name, Class<?>... parameters) throws Exception {
		Class<PersonRepository> repositoryClass = PersonRepository.class;

		return repositoryClass.getMethod(name, parameters);
	}

	interface PersonRepository extends Repository<Person, Long> {

		@Query(CUSTOM_CYPHER_QUERY)
		List<Person> annotatedQuery();

		@Query
		List<Person> invalidAnnotatedQuery();
	}

	class Person {
	}

}
