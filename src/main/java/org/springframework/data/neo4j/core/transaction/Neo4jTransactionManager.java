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

import org.apiguardian.api.API;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * Dedicated {@link org.springframework.transaction.PlatformTransactionManager} for native Neo4j transactions. This
 * transaction manager will synchronize a pair of a native Neo4j session/transaction with the transaction.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jTransactionManager extends AbstractPlatformTransactionManager implements ApplicationContextAware {

	/**
	 * The underlying driver, which is also the synchronisation object.
	 */
	private final Driver driver;

	/**
	 * Database name provider.
	 */
	private final DatabaseSelectionProvider databaseSelectionProvider;

	private final Neo4jBookmarkManager bookmarkManager;

	/**
	 * This will create a transaction manager for the default database.
	 *
	 * @param driver A driver instance
	 */
	public Neo4jTransactionManager(Driver driver) {

		this(driver, DatabaseSelectionProvider.getDefaultSelectionProvider());
	}

	/**
	 * This will create a transaction manager targeting whatever the database selection provider determines.
	 *
	 * @param driver A driver instance
	 * @param databaseSelectionProvider The database selection provider to determine the database in which the transactions should happen
	 */
	public Neo4jTransactionManager(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {

		this(driver, databaseSelectionProvider, Neo4jBookmarkManager.create());
	}

	/**
	 * This constructor can be used to configure the bookmark manager being used. It is useful when you need to seed
	 * the bookmark manager or if you want to capture new bookmarks.
	 *
	 * @param driver A driver instance
	 * @param databaseSelectionProvider The database selection provider to determine the database in which the transactions should happen
	 * @param bookmarkManager A bookmark manager
	 */
	public Neo4jTransactionManager(Driver driver, DatabaseSelectionProvider databaseSelectionProvider, Neo4jBookmarkManager bookmarkManager) {

		this.driver = driver;
		this.databaseSelectionProvider = databaseSelectionProvider;
		this.bookmarkManager = bookmarkManager;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.bookmarkManager.setApplicationEventPublisher(applicationContext);
	}

	/**
	 * This methods provides a native Neo4j transaction to be used from within a {@link Neo4jClient}. In most cases this
	 * the native transaction will be controlled from the Neo4j specific
	 * {@link org.springframework.transaction.PlatformTransactionManager}. However, SDN provides support for other
	 * transaction managers as well. This methods registers a session synchronization in such cases on the foreign
	 * transaction manager.
	 *
	 * @param driver The driver that has been used as a synchronization object.
	 * @param targetDatabase The target database
	 * @return An optional managed transaction or {@literal null} if the method hasn't been called inside an ongoing
	 *         Spring transaction
	 */
	public static @Nullable Transaction retrieveTransaction(final Driver driver, @Nullable final String targetDatabase) {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}

		// Check whether we have a transaction managed by a Neo4j transaction manager
		Neo4jTransactionHolder connectionHolder = (Neo4jTransactionHolder) TransactionSynchronizationManager
				.getResource(driver);

		if (connectionHolder != null) {
			Transaction optionalOngoingTransaction = connectionHolder.getTransaction(targetDatabase);

			if (optionalOngoingTransaction != null) {
				return optionalOngoingTransaction;
			}

			throw new IllegalStateException(
					Neo4jTransactionUtils
							.formatOngoingTxInAnotherDbErrorMessage(connectionHolder.getDatabaseName(), targetDatabase));
		}

		// Otherwise we open a session and synchronize it.
		Session session = driver.session(Neo4jTransactionUtils.defaultSessionConfig(targetDatabase));
		Transaction transaction = session.beginTransaction(TransactionConfig.empty());
		// Manually create a new synchronization
		connectionHolder = new Neo4jTransactionHolder(new Neo4jTransactionContext(targetDatabase), session, transaction);
		connectionHolder.setSynchronizedWithTransaction(true);

		TransactionSynchronizationManager
				.registerSynchronization(new Neo4jSessionSynchronization(connectionHolder, driver));

		TransactionSynchronizationManager.bindResource(driver, connectionHolder);
		return connectionHolder.getTransaction(targetDatabase);
	}

	private static Neo4jTransactionObject extractNeo4jTransaction(Object transaction) {

		Assert.isInstanceOf(Neo4jTransactionObject.class, transaction,
				() -> String.format("Expected to find a %s but it turned out to be %s.", Neo4jTransactionObject.class,
						transaction.getClass()));

		return (Neo4jTransactionObject) transaction;
	}

	private static Neo4jTransactionObject extractNeo4jTransaction(DefaultTransactionStatus status) {

		return extractNeo4jTransaction(status.getTransaction());
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {

		Neo4jTransactionHolder resourceHolder = (Neo4jTransactionHolder) TransactionSynchronizationManager
				.getResource(driver);
		return new Neo4jTransactionObject(resourceHolder);
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {

		return extractNeo4jTransaction(transaction).hasResourceHolder();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(transaction);

		TransactionConfig transactionConfig = Neo4jTransactionUtils.createTransactionConfigFrom(definition);
		boolean readOnly = definition.isReadOnly();

		TransactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);

		try {
			// Prepare configuration data
			Neo4jTransactionContext context = new Neo4jTransactionContext(
					databaseSelectionProvider.getDatabaseSelection().getValue(), bookmarkManager.getBookmarks());

			// Configure and open session together with a native transaction
			Session session = this.driver.session(
					Neo4jTransactionUtils.sessionConfig(readOnly, context.getBookmarks(), context.getDatabaseName()));
			Transaction nativeTransaction = session.beginTransaction(transactionConfig);

			// Synchronize on that
			Neo4jTransactionHolder transactionHolder = new Neo4jTransactionHolder(context, session, nativeTransaction);
			transactionHolder.setSynchronizedWithTransaction(true);
			transactionObject.setResourceHolder(transactionHolder);

			TransactionSynchronizationManager.bindResource(this.driver, transactionHolder);
		} catch (Exception ex) {
			throw new TransactionSystemException(String.format("Could not open a new Neo4j session: %s", ex.getMessage()),
					ex);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) throws TransactionException {

		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(transaction);
		transactionObject.setResourceHolder(null);

		return TransactionSynchronizationManager.unbindResource(driver);
	}

	@Override
	protected void doResume(@Nullable Object transaction, Object suspendedResources) {

		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(transaction);
		transactionObject.setResourceHolder((Neo4jTransactionHolder) suspendedResources);

		TransactionSynchronizationManager.bindResource(driver, suspendedResources);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {

		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(status);
		Neo4jTransactionHolder transactionHolder = transactionObject.getRequiredResourceHolder();
		Bookmark lastBookmark = transactionHolder.commit();
		this.bookmarkManager.updateBookmarks(transactionHolder.getBookmarks(), lastBookmark);
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {

		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(status);
		transactionObject.getRequiredResourceHolder().rollback();
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {

		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(status);
		transactionObject.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {

		Neo4jTransactionObject transactionObject = extractNeo4jTransaction(transaction);
		transactionObject.getRequiredResourceHolder().close();
		transactionObject.setResourceHolder(null);
		TransactionSynchronizationManager.unbindResource(driver);
	}

	static class Neo4jTransactionObject implements SmartTransactionObject {

		private static final String RESOURCE_HOLDER_NOT_PRESENT_MESSAGE = "Neo4jConnectionHolder is required but not present. o_O";

		// The resource holder is null when the call to TransactionSynchronizationManager.getResource
		// in Neo4jTransactionManager.doGetTransaction didn't return a corresponding resource holder.
		// If it is null, there's no existing session / transaction.
		@Nullable private Neo4jTransactionHolder resourceHolder;

		Neo4jTransactionObject(@Nullable Neo4jTransactionHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		/**
		 * Usually called in {@link #doBegin(Object, TransactionDefinition)} which is called when there's no existing
		 * transaction.
		 *
		 * @param resourceHolder A newly created resource holder with a fresh drivers session,
		 */
		void setResourceHolder(@Nullable Neo4jTransactionHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		/**
		 * @return {@literal true} if a {@link Neo4jTransactionHolder} is set.
		 */
		boolean hasResourceHolder() {
			return resourceHolder != null;
		}

		Neo4jTransactionHolder getRequiredResourceHolder() {

			Assert.state(hasResourceHolder(), RESOURCE_HOLDER_NOT_PRESENT_MESSAGE);
			return resourceHolder;
		}

		void setRollbackOnly() {

			getRequiredResourceHolder().setRollbackOnly();
		}

		@Override
		public boolean isRollbackOnly() {
			return this.hasResourceHolder() && this.resourceHolder.isRollbackOnly();
		}

		@Override
		public void flush() {

			TransactionSynchronizationUtils.triggerFlush();
		}
	}
}
