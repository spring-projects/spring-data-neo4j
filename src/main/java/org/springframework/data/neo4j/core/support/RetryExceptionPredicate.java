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

import java.util.Set;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.neo4j.driver.exceptions.DiscoveryException;
import org.neo4j.driver.exceptions.RetryableException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;
import org.neo4j.driver.exceptions.TransientException;

import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.transaction.TransactionSystemException;

/**
 * A predicate indicating {@literal true} for {@link Throwable throwables} that can be
 * safely retried and {@literal false} in any other case. This predicate can be used for
 * example with Resilience4j.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class RetryExceptionPredicate implements Predicate<Throwable> {

	/**
	 * A known message indicating retryable errors before they had been marked retryable.
	 */
	public static final String TRANSACTION_MUST_BE_OPEN_BUT_HAS_ALREADY_BEEN_CLOSED = "Transaction must be open, but has already been closed";

	/**
	 * A known message indicating retryable errors before they had been marked retryable.
	 */
	public static final String SESSION_MUST_BE_OPEN_BUT_HAS_ALREADY_BEEN_CLOSED = "Session must be open, but has already been closed";

	private static final Set<String> RETRYABLE_ILLEGAL_STATE_MESSAGES = Set
		.of(TRANSACTION_MUST_BE_OPEN_BUT_HAS_ALREADY_BEEN_CLOSED, SESSION_MUST_BE_OPEN_BUT_HAS_ALREADY_BEEN_CLOSED);

	@Override
	public boolean test(Throwable throwable) {

		if (throwable instanceof RetryableException) {
			return true;
		}

		if (throwable instanceof TransactionSystemException && throwable.getCause() != null) {
			throwable = throwable.getCause();
		}

		if (throwable instanceof IllegalStateException) {
			String msg = throwable.getMessage();
			return msg != null && RETRYABLE_ILLEGAL_STATE_MESSAGES.contains(msg);
		}

		Throwable ex = throwable;
		if (throwable instanceof TransientDataAccessResourceException
				|| throwable instanceof TransactionSystemException) {
			ex = throwable.getCause();
		}

		if (ex instanceof TransientException) {
			String code = ((TransientException) ex).code();
			return !("Neo.TransientError.Transaction.Terminated".equals(code)
					|| "Neo.TransientError.Transaction.LockClientStopped".equals(code));
		}
		else {
			return ex instanceof SessionExpiredException || ex instanceof ServiceUnavailableException
					|| ex instanceof DiscoveryException;
		}
	}

}
