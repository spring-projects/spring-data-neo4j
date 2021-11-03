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
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.data.neo4j.core.UserSelectionProvider;
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
import org.springframework.util.StringUtils;

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
	 * Start building a new transaction manager for the given driver instance.
	 * @param driver A fixed driver instance.
	 * @return A builder for a transaction manager
	 */
	@API(status = API.Status.STABLE, since = "6.2")
	public static Builder with(Driver driver) {

		return new Builder(driver);
	}

	/**
	 * A builder for {@link Neo4jTransactionManager}.
	 */
	@API(status = API.Status.STABLE, since = "6.2")
	@SuppressWarnings("HiddenField")
	public static final class Builder {

		private final Driver driver;

		@Nullable
		private DatabaseSelectionProvider databaseSelectionProvider;

		@Nullable
		private UserSelectionProvider userSelectionProvider;

		@Nullable
		private Neo4jBookmarkManager bookmarkManager;

		private Builder(Driver driver) {
			this.driver = driver;
		}

		/**
		 * Configures the database selection provider. Make sure to use the same instance as for a possible
		 * {@link org.springframework.data.neo4j.core.Neo4jClient}. During runtime, it will be checked if a call is made
		 * for the same database when happening in a managed transaction.
		 *
		 * @param databaseSelectionProvider The database selection provider
		 * @return The builder
		 */
		public Builder withDatabaseSelectionProvider(@Nullable DatabaseSelectionProvider databaseSelectionProvider) {
			this.databaseSelectionProvider = databaseSelectionProvider;
			return this;
		}

		/**
		 * Configures a provider for impersonated users. Make sure to use the same instance as for a possible
		 * {@link org.springframework.data.neo4j.core.Neo4jClient}. During runtime, it will be checked if a call is made
		 * for the same user when happening in a managed transaction.
		 *
		 * @param userSelectionProvider The provider for impersonated users
		 * @return The builder
		 */
		public Builder withUserSelectionProvider(@Nullable UserSelectionProvider userSelectionProvider) {
			this.userSelectionProvider = userSelectionProvider;
			return this;
		}

		public Builder withBookmarkManager(@Nullable Neo4jBookmarkManager bookmarkManager) {
			this.bookmarkManager = bookmarkManager;
			return this;
		}

		public Neo4jTransactionManager build() {
			return new Neo4jTransactionManager(this);
		}
	}

	/**
	 * The underlying driver, which is also the synchronisation object.
	 */
	private final Driver driver;

	/**
	 * Database name provider.
	 */
	private final DatabaseSelectionProvider databaseSelectionProvider;

	/**
	 * Provider for user impersonation.
	 */
	private final UserSelectionProvider userSelectionProvider;

	private final Neo4jBookmarkManager bookmarkManager;

	/**
	 * This will create a transaction manager for the default database.
	 *
	 * @param driver A driver instance
	 */
	public Neo4jTransactionManager(Driver driver) {

		this(with(driver));
	}

	/**
	 * This will create a transaction manager targeting whatever the database selection provider determines.
	 *
	 * @param driver A driver instance
	 * @param databaseSelectionProvider The database selection provider to determine the database in which the transactions should happen
	 */
	public Neo4jTransactionManager(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {

		this(with(driver).withDatabaseSelectionProvider(databaseSelectionProvider));
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

		this(with(driver).withDatabaseSelectionProvider(databaseSelectionProvider).withBookmarkManager(bookmarkManager));
	}

	private Neo4jTransactionManager(Builder builder) {

		this.driver = builder.driver;
		this.databaseSelectionProvider = builder.databaseSelectionProvider == null ?
				DatabaseSelectionProvider.getDefaultSelectionProvider() :
				builder.databaseSelectionProvider;
		this.userSelectionProvider = builder.userSelectionProvider == null ?
				UserSelectionProvider.getDefaultSelectionProvider() :
				builder.userSelectionProvider;
		this.bookmarkManager =
				builder.bookmarkManager == null ? Neo4jBookmarkManager.create() : builder.bookmarkManager;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.bookmarkManager.setApplicationEventPublisher(applicationContext);
	}

	/**
	 * @param driver The driver that has been used as a synchronization object.
	 * @param targetDatabase The target database
	 * @return An optional managed transaction or {@literal null} if the method hasn't been called inside an ongoing
	 *         Spring transaction
	 * @see #retrieveTransaction(Driver, DatabaseSelection, UserSelection)
	 * @deprecated since 6.2, use #retrieveTransaction(Driver, DatabaseSelection, UserSelection)
	 */
	@Deprecated
	public static @Nullable Transaction retrieveTransaction(final Driver driver, @Nullable final String targetDatabase) {

		return retrieveTransaction(driver, StringUtils.hasText(targetDatabase) ? DatabaseSelection.byName(targetDatabase) : DatabaseSelection.undecided(), UserSelection.connectedUser());
	}

	/**
	 * This method provides a native Neo4j transaction to be used from within a {@link org.springframework.data.neo4j.core.Neo4jClient}.
	 * In most cases this the native transaction will be controlled from the Neo4j specific {@link org.springframework.transaction.PlatformTransactionManager}.
	 * However, SDN provides support for other transaction managers as well. This method registers a session synchronization
	 * in such cases on the foreign transaction manager.
	 *
	 * @param driver The driver that has been used as a synchronization object.
	 * @param targetDatabase The target database
	 * @param asUser The user for which the tx is being retrieved
	 * @return An optional managed transaction or {@literal null} if the method hasn't been called inside an ongoing
	 *         Spring transaction
	 */
	public static @Nullable Transaction retrieveTransaction(
			final Driver driver,
			final DatabaseSelection targetDatabase,
			final UserSelection asUser
	) {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}

		// Check whether we have a transaction managed by a Neo4j transaction manager
		Neo4jTransactionHolder connectionHolder = (Neo4jTransactionHolder) TransactionSynchronizationManager
				.getResource(driver);

		if (connectionHolder != null) {
			Transaction optionalOngoingTransaction = connectionHolder.getTransaction(targetDatabase, asUser);

			if (optionalOngoingTransaction != null) {
				return optionalOngoingTransaction;
			}

			throw new IllegalStateException(
					Neo4jTransactionUtils.formatOngoingTxInAnotherDbErrorMessage(
							connectionHolder.getDatabaseSelection(), targetDatabase,
							connectionHolder.getUserSelection(), asUser));
		}

		// Otherwise we open a session and synchronize it.
		Session session = driver.session(Neo4jTransactionUtils.defaultSessionConfig(targetDatabase, asUser));
		Transaction transaction = session.beginTransaction(TransactionConfig.empty());
		// Manually create a new synchronization
		connectionHolder = new Neo4jTransactionHolder(new Neo4jTransactionContext(targetDatabase, asUser), session, transaction);
		connectionHolder.setSynchronizedWithTransaction(true);

		TransactionSynchronizationManager
				.registerSynchronization(new Neo4jSessionSynchronization(connectionHolder, driver));

		TransactionSynchronizationManager.bindResource(driver, connectionHolder);
		return connectionHolder.getTransaction(targetDatabase, asUser);
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
					databaseSelectionProvider.getDatabaseSelection(), userSelectionProvider.getUserSelection(), bookmarkManager.getBookmarks());

			// Configure and open session together with a native transaction
			Session session = this.driver.session(
					Neo4jTransactionUtils.sessionConfig(readOnly, context.getBookmarks(), context.getDatabaseSelection(), context.getUserSelection()));
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
