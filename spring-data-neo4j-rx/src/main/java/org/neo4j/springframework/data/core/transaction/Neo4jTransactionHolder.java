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
package org.neo4j.springframework.data.core.transaction;

import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionUtils.*;

import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Neo4j specific {@link ResourceHolderSupport resource holder}, wrapping a {@link org.neo4j.driver.Transaction}.
 * {@link Neo4jTransactionManager} binds instances of this class to the thread.
 * <p>
 * <strong>Note:</strong> Intended for internal usage only.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
class Neo4jTransactionHolder extends ResourceHolderSupport {

	private final String databaseName;
	private final Session session;
	private final Transaction transaction;

	Neo4jTransactionHolder(String databaseName, Session session, Transaction transaction) {

		this.databaseName = databaseName;
		this.session = session;
		this.transaction = transaction;
	}

	/**
	 * Returns the transaction if it has been opened in a session for the requested database or an empty optional.
	 *
	 * @param inDatabase selected database to use
	 * @return An optional, ongoing transaction.
	 */
	@Nullable Transaction getTransaction(String inDatabase) {
		return namesMapToTheSameDatabase(this.databaseName, inDatabase) ? transaction : null;
	}

	void commit() {

		Assert.state(hasActiveTransaction(), "Transaction must be open, but has already been closed.");
		Assert.state(!isRollbackOnly(), "Resource must not be marked as rollback only.");

		transaction.commit();
		transaction.close();
	}

	void rollback() {

		Assert.state(hasActiveTransaction(), "Transaction must be open, but has already been closed.");

		transaction.rollback();
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
		transaction.rollback();
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

	String getDatabaseName() {
		return databaseName;
	}
}
