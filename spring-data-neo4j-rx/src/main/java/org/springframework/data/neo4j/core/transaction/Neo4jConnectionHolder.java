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
package org.springframework.data.neo4j.core.transaction;

import java.util.Optional;

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
class Neo4jConnectionHolder extends ResourceHolderSupport {

	private final String databaseName;

	private final Session session;

	private final Transaction transaction;

	Neo4jConnectionHolder(String databaseName, Session session) {
		this(databaseName, session, TransactionConfig.empty());
	}

	Neo4jConnectionHolder(String databaseName, Session session, TransactionConfig transactionConfig) {

		this.databaseName = databaseName;
		this.session = session;
		this.transaction = this.session.beginTransaction(transactionConfig);
	}

	/**
	 * Returns the transaction if it has been opened in a session for the requested database.
	 *
	 * @param inDatabase
	 * @return
	 */
	Transaction getTransaction(String inDatabase) {

		return namesMapToTheSameDatabase(this.databaseName, inDatabase) ? transaction : null;
	}

	static boolean namesMapToTheSameDatabase(String name1, String name2) {
		String d1 = Optional.ofNullable(name1).orElse(Neo4jTransactionUtils.DEFAULT_DATABASE_NAME);
		String d2 = Optional.ofNullable(name2).orElse(Neo4jTransactionUtils.DEFAULT_DATABASE_NAME);

		return d1.equals(d2);
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
