/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.transaction;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Test;
import org.neo4j.ogm.exception.CypherException;
import org.neo4j.ogm.exception.core.BaseClassNotFoundException;
import org.neo4j.ogm.exception.core.NotFoundException;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.neo4j.exception.UncategorizedNeo4jException;

/**
 * @author Mark Angrish
 */
public class SessionFactoryUtilsTests {

	@Test
	public void translatNotFoundException() {
		NotFoundException nfe = new NotFoundException("Not Found");
		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(nfe);
		expectExceptionWithCauseMessage(translatedException, DataRetrievalFailureException.class, null);
	}

	@Test
	public void translateMappingException() {
		BaseClassNotFoundException bcnfe = new BaseClassNotFoundException("Classname");
		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(bcnfe);
		expectExceptionWithCauseMessage(translatedException, InvalidDataAccessApiUsageException.class, null);
	}

	@Test
	public void translateToDataIntegrityException() {
		CypherException ce = new CypherException("Cypher Exception caused by:",
				"Neo.ClientError.Schema.ConstraintValidationFailed", "The reason");
		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(ce);
		expectExceptionWithCauseMessage(translatedException, DataIntegrityViolationException.class, null);
	}

	@Test
	public void translateToUncategorizedNeo4jException() {

		CypherException ce = new CypherException("Cypher Exception caused by:", "A.New.Neo.Error", "The reason");

		DataAccessException translatedException = SessionFactoryUtils.convertOgmAccessException(ce);

		expectExceptionWithCauseMessage(translatedException, UncategorizedNeo4jException.class);
	}

	@Test
	public void translateUnsupportedException() {

		RuntimeException exception = new RuntimeException();
		assertThat(SessionFactoryUtils.convertOgmAccessException(exception), is(nullValue()));
	}

	private static void expectExceptionWithCauseMessage(NestedRuntimeException e,
			Class<? extends NestedRuntimeException> type) {
		expectExceptionWithCauseMessage(e, type, null);
	}

	private static void expectExceptionWithCauseMessage(NestedRuntimeException e,
			Class<? extends NestedRuntimeException> type, String message) {

		assertThat(e, is(instanceOf(type)));

		if (message != null) {
			assertThat(e.getRootCause(), is(notNullValue()));
			assertThat(e.getRootCause().getMessage(), containsString(message));
		}
	}
}
