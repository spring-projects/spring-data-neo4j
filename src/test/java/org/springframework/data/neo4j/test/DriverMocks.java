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
package org.springframework.data.neo4j.test;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Some preconfigured driver mocks, mainly used to for Spring Integration tests where the
 * behaviour of configuration and integration with Spring is tested and not with the
 * database.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
public final class DriverMocks {

	private DriverMocks() {
	}

	/**
	 * @return An instance usable in a test where an open session with an ongoing
	 * transaction is required.
	 */
	public static Driver withOpenSessionAndTransaction() {

		Transaction transaction = mock(Transaction.class);
		given(transaction.isOpen()).willReturn(true);

		Session session = mock(Session.class);
		given(session.isOpen()).willReturn(true);
		given(session.beginTransaction(any(TransactionConfig.class))).willReturn(transaction);

		Driver driver = mock(Driver.class);
		given(driver.session(any(SessionConfig.class))).willReturn(session);
		return driver;
	}

	public static Driver withOpenReactiveSessionAndTransaction() {

		ReactiveTransaction transaction = mock(ReactiveTransaction.class);

		ReactiveSession session = mock(ReactiveSession.class);
		given(session.beginTransaction(any(TransactionConfig.class))).willReturn(Mono.just(transaction));

		Driver driver = mock(Driver.class);
		given(driver.session(eq(ReactiveSession.class), any(SessionConfig.class))).willReturn(session);
		return driver;
	}

}
