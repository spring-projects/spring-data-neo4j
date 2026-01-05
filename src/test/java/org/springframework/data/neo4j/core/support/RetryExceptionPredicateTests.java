/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.core.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.commons.util.ReflectionUtils;
import org.neo4j.driver.exceptions.DiscoveryException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;

import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.neo4j.core.Neo4jPersistenceExceptionTranslator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
class RetryExceptionPredicateTests {

	@ParameterizedTest
	@ValueSource(strings = { "Transaction must be open, but has already been closed",
			"Session must be open, but has already been closed" })
	void shouldRetryOnSomeIllegalStateExceptions(String msg) {

		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(new IllegalStateException(msg))).isTrue();
	}

	@Test
	void shouldNotRetryOnRandomIllegalStateExceptions() {

		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(new IllegalStateException())).isFalse();
	}

	@Test // GH-2311
	void shouldRetryDiscoveryExceptions() {

		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(new DiscoveryException("Broken", new RuntimeException()))).isTrue();
	}

	@Test
	void shouldPlayWellWithExceptionTranslator() {

		Neo4jPersistenceExceptionTranslator translator = new Neo4jPersistenceExceptionTranslator();
		Exception e = translator.translateExceptionIfPossible(new SessionExpiredException("Schade"));
		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(e)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(classes = { SessionExpiredException.class, ServiceUnavailableException.class })
	void shouldRetryOnObviousRetryableExceptions(Class<? extends Exception> typeOfException) {

		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(ReflectionUtils.newInstance(typeOfException, "msg"))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(classes = { TransientDataAccessResourceException.class, NullPointerException.class })
	void shouldNotRetryOnRandomExceptions() {

		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(new IllegalStateException())).isFalse();
	}

	@Test
	void shouldExtractCause() {

		TransientDataAccessResourceException ex = new TransientDataAccessResourceException("msg",
				new ServiceUnavailableException("msg"));

		RetryExceptionPredicate predicate = new RetryExceptionPredicate();
		assertThat(predicate.test(ex)).isTrue();
	}

}
