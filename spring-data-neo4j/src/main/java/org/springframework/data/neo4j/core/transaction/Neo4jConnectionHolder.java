/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.transaction;

import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Neo4j specific {@link ResourceHolderSupport resource holder}, wrapping a {@link org.neo4j.driver.Transaction}.
 * {@link Neo4jTransactionManager} binds instances of this class to the thread.
 * <p>
 * <strong>Note:</strong> Intended for internal usage only.
 *
 * @author Michael J. Simons
 */
public class Neo4jConnectionHolder extends ResourceHolderSupport {

	private final Session session;

	private final Transaction transaction;

	Neo4jConnectionHolder(Session session) {
		this(session, TransactionConfig.empty());
	}

	Neo4jConnectionHolder(Session session, TransactionConfig transactionConfig) {
		this.session = session;

		this.transaction = this.session.beginTransaction(transactionConfig);
	}

	Transaction getTransaction() {
		return transaction;
	}

	void commit() {

		Assert.state(hasActiveTransaction(), "Transaction must be open, but has already been closed.");
		Assert.state(!isRollbackOnly(), "Resource msut not be marked as rollback only.");

		transaction.success();
		transaction.close();
	}

	void rollback() {

		Assert.state(hasActiveTransaction(), "Transaction must be open, but has already been closed.");

		transaction.failure();
		transaction.close();
	}

	void close() {

		Assert.state(hasActiveSession(), "Session must be open, but has already been closed.");

		if (hasActiveTransaction()) {
			transaction.close();
		}
		session.close();
	}

	@Override
	public void setRollbackOnly() {
		super.setRollbackOnly();
	}

	@Override
	public void resetRollbackOnly() {

		throw new UnsupportedOperationException();
	}

	boolean hasActiveSession() {

		return session.isOpen();
	}

	boolean hasActiveTransaction() {

		return transaction.isOpen();
	}
}
