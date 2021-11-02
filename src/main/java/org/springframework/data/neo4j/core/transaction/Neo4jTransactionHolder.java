/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.core.transaction;

import java.util.Collection;

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
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
 * @since 6.0
 */
final class Neo4jTransactionHolder extends ResourceHolderSupport {

	private final Neo4jTransactionContext context;
	/**
	 * The ongoing session...
	 */
	private final Session session;
	/**
	 * The driver's transaction as the second building block of what to synchronize our transaction against.
	 */
	private final Transaction transaction;

	Neo4jTransactionHolder(Neo4jTransactionContext context, Session session, Transaction transaction) {

		this.context = context;
		this.session = session;
		this.transaction = transaction;
	}

	/**
	 * Returns the transaction if it has been opened in a session for the requested database or an empty optional.
	 *
	 * @param inDatabase selected database to use
	 * @param asUser impersonated user if any
	 * @return An optional, ongoing transaction.
	 */
	@Nullable
	Transaction getTransaction(DatabaseSelection inDatabase, UserSelection asUser) {
		return this.context.isForDatabaseAndUser(inDatabase, asUser) ? transaction : null;
	}

	@Nullable
	Bookmark commit() {

		Assert.state(hasActiveTransaction(), "Transaction must be open, but has already been closed.");
		Assert.state(!isRollbackOnly(), "Resource must not be marked as rollback only.");

		transaction.commit();
		transaction.close();

		return session.lastBookmark();
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

	boolean hasActiveSession() {

		return session.isOpen();
	}

	boolean hasActiveTransaction() {

		return transaction.isOpen();
	}

	DatabaseSelection getDatabaseSelection() {
		return context.getDatabaseSelection();
	}

	UserSelection getUserSelection() {
		return context.getUserSelection();
	}

	Collection<Bookmark> getBookmarks() {
		return context.getBookmarks();
	}
}
