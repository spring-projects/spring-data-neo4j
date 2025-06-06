/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.exceptions.value.LossyCoercion;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class Neo4jPersistenceExceptionTranslatorTests {

	@Test
	void shouldHandleNullErrorCode() {

		Neo4jPersistenceExceptionTranslator translator = new Neo4jPersistenceExceptionTranslator();
		DataAccessException dataAccessException = translator
			.translateExceptionIfPossible(new LossyCoercion("Long", "Int"));
		assertThat(dataAccessException).isNotNull().isInstanceOf(InvalidDataAccessApiUsageException.class);
		assertThat(dataAccessException.getMessage())
			.startsWith("Cannot coerce Long to Int without losing precision; Error code 'N/A'");
	}

	@Test
	void shouldKeepErrorCodeIntact() {

		Neo4jPersistenceExceptionTranslator translator = new Neo4jPersistenceExceptionTranslator();
		DataAccessException dataAccessException = translator.translateExceptionIfPossible(
				new ClientException("Neo.ClientError.Statement.EntityNotFound", "Something went wrong."));
		assertThat(dataAccessException).isNotNull().isInstanceOf(InvalidDataAccessResourceUsageException.class);
		assertThat(dataAccessException.getMessage())
			.startsWith("Something went wrong.; Error code 'Neo.ClientError.Statement.EntityNotFound'");
	}

}
