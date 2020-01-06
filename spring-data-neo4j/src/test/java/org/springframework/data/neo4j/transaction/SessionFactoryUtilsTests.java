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
package org.springframework.data.neo4j.transaction;

import org.junit.Test;
import org.neo4j.ogm.exception.CypherException;
import org.neo4j.ogm.exception.core.BaseClassNotFoundException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.exception.UncategorizedNeo4jException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Angrish
 * @author Michael J. Simons
 */
public class SessionFactoryUtilsTests {

	@Test
	public void translateMappingException() {
		BaseClassNotFoundException bcnfe = new BaseClassNotFoundException("Classname");
		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(bcnfe);
		expectExceptionWithCauseMessage(translatedException, InvalidDataAccessApiUsageException.class, null);
	}

	@Test
	public void translateToDataIntegrityException() {
		CypherException ce = new CypherException("Neo.ClientError.Schema.ConstraintValidationFailed",
				"Cypher Exception caused by:");
		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(ce);
		expectExceptionWithCauseMessage(translatedException, DataIntegrityViolationException.class, null);
	}

	@Test
	public void translateToUncategorizedNeo4jException() {

		CypherException ce = new CypherException("A.New.Neo.Error", "Cypher Exception caused by:");

		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(ce);

		expectExceptionWithCauseMessage(translatedException, UncategorizedNeo4jException.class);
	}

	@Test
	public void translateUnsupportedException() {

		RuntimeException exception = new RuntimeException();
		assertThat(SessionFactoryUtils.convertOgmAccessException(exception)).isNull();
	}

	private static void expectExceptionWithCauseMessage(NestedRuntimeException e,
			Class<? extends NestedRuntimeException> type) {
		expectExceptionWithCauseMessage(e, type, null);
	}

	private static void expectExceptionWithCauseMessage(NestedRuntimeException e,
			Class<? extends NestedRuntimeException> type, String message) {

		assertThat(e).isInstanceOf(type);

		if (message != null) {
			assertThat(e.getRootCause()).isNotNull();
			assertThat(e.getRootCause().getMessage()).contains(message);
		}
	}
}
