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

import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveUserSelectionProvider;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.lang.Nullable;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class ReactiveNeo4jTransactionManager extends AbstractReactiveTransactionManager implements ApplicationContextAware {

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
	 * A builder for {@link ReactiveNeo4jTransactionManager}.
	 */
	@API(status = API.Status.STABLE, since = "6.2")
	@SuppressWarnings("HiddenField")
	public static final class Builder {

		private final Driver driver;

		@Nullable
		private ReactiveDatabaseSelectionProvider databaseSelectionProvider;

		@Nullable
		private ReactiveUserSelectionProvider userSelectionProvider;

		@Nullable
		private Neo4jBookmarkManager bookmarkManager;

		private Builder(Driver driver) {
			this.driver = driver;
		}

		/**
		 * Configures the database selection provider. Make sure to use the same instance as for a possible
		 * {@link org.springframework.data.neo4j.core.ReactiveNeo4jClient}. During runtime, it will be checked if a call is made
		 * for the same database when happening in a managed transaction.
		 *
		 * @param databaseSelectionProvider The database selection provider
		 * @return The builder
		 */
		public Builder withDatabaseSelectionProvider(@Nullable ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
			this.databaseSelectionProvider = databaseSelectionProvider;
			return this;
		}

		/**
		 * Configures a provider for impersonated users. Make sure to use the same instance as for a possible
		 * {@link org.springframework.data.neo4j.core.ReactiveNeo4jClient}. During runtime, it will be checked if a call is made
		 * for the same user when happening in a managed transaction.
		 *
		 * @param userSelectionProvider The provider for impersonated users
		 * @return The builder
		 */
		public Builder withUserSelectionProvider(@Nullable ReactiveUserSelectionProvider userSelectionProvider) {
			this.userSelectionProvider = userSelectionProvider;
			return this;
		}

		public Builder withBookmarkManager(@Nullable Neo4jBookmarkManager bookmarkManager) {
			this.bookmarkManager = bookmarkManager;
			return this;
		}

		public ReactiveNeo4jTransactionManager build() {
			return new ReactiveNeo4jTransactionManager(this);
		}
	}

	/**
	 * The underlying driver, which is also the synchronisation object.
	 */
	private final Driver driver;

	/**
	 * Database name provider.
	 */
	private final ReactiveDatabaseSelectionProvider databaseSelectionProvider;

	/**
	 * Provider for user impersonation.
	 */
	private final ReactiveUserSelectionProvider userSelectionProvider;

	private final Neo4jBookmarkManager bookmarkManager;

	/**
	 * This will create a transaction manager for the default database.
	 *
	 * @param driver A driver instance
	 */
	public ReactiveNeo4jTransactionManager(Driver driver) {

		this(with(driver));
	}

	/**
	 * This will create a transaction manager targeting whatever the database selection provider determines.
	 *
	 * @param driver A driver instance
	 * @param databaseSelectionProvider The database selection provider to determine the database in which the transactions should happen
	 */
	public ReactiveNeo4jTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

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
	public ReactiveNeo4jTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider, Neo4jBookmarkManager bookmarkManager) {

		this(with(driver).withDatabaseSelectionProvider(databaseSelectionProvider).withBookmarkManager(bookmarkManager));
	}

	private ReactiveNeo4jTransactionManager(Builder builder) {

		this.driver = builder.driver;
		this.databaseSelectionProvider = builder.databaseSelectionProvider == null ?
				ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider() :
				builder.databaseSelectionProvider;
		this.userSelectionProvider = builder.userSelectionProvider == null ?
				ReactiveUserSelectionProvider.getDefaultSelectionProvider() :
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
	 * @see #retrieveReactiveTransaction(Driver, DatabaseSelection, UserSelection)
	 * @deprecated since 6.2, use #retrieveReactiveTransaction(Driver, DatabaseSelection, UserSelection)
	 */
	@Deprecated
	public static Mono<RxTransaction> retrieveReactiveTransaction(final Driver driver, @Nullable final String targetDatabase) {

		return retrieveReactiveTransaction(driver, StringUtils.hasText(targetDatabase) ? DatabaseSelection.byName(targetDatabase) : DatabaseSelection.undecided(), UserSelection.connectedUser());
	}

	public static Mono<RxTransaction> retrieveReactiveTransaction(
			final Driver driver,
			final DatabaseSelection targetDatabase,
			final UserSelection asUser
	) {

		return TransactionSynchronizationManager.forCurrentTransaction() // Do we have a Transaction context?
				// Bail out early if synchronization between transaction managers is not active
				.filter(TransactionSynchronizationManager::isSynchronizationActive).flatMap(tsm -> {
					// Get an existing holder
					ReactiveNeo4jTransactionHolder existingTxHolder = (ReactiveNeo4jTransactionHolder) tsm.getResource(driver);

					// And use it if there is any
					if (existingTxHolder != null) {
						return Mono.just(existingTxHolder);
					}

					// Otherwise open up a new native transaction
					return Mono.defer(() -> {
						RxSession session = driver.rxSession(Neo4jTransactionUtils.defaultSessionConfig(targetDatabase, asUser));
						return Mono.from(session.beginTransaction(TransactionConfig.empty())).map(tx -> {

							ReactiveNeo4jTransactionHolder newConnectionHolder = new ReactiveNeo4jTransactionHolder(
									new Neo4jTransactionContext(targetDatabase, asUser), session, tx);
							newConnectionHolder.setSynchronizedWithTransaction(true);

							tsm.registerSynchronization(new ReactiveNeo4jSessionSynchronization(tsm, newConnectionHolder, driver));

							tsm.bindResource(driver, newConnectionHolder);
							return newConnectionHolder;
						});
					});
				}).map(connectionHolder -> {
					RxTransaction transaction = connectionHolder.getTransaction(targetDatabase, asUser);
					if (transaction == null) {
						throw new IllegalStateException(
								Neo4jTransactionUtils.formatOngoingTxInAnotherDbErrorMessage(
										connectionHolder.getDatabaseSelection(), targetDatabase,
										connectionHolder.getUserSelection(), asUser));
					}
					return transaction;
				})
				// If not, then just don't open a transaction
				.onErrorResume(NoTransactionException.class, nte -> Mono.empty());
	}

	private static ReactiveNeo4jTransactionObject extractNeo4jTransaction(Object transaction) {

		Assert.isInstanceOf(ReactiveNeo4jTransactionObject.class, transaction,
				() -> String.format("Expected to find a %s but it turned out to be %s.", ReactiveNeo4jTransactionObject.class,
						transaction.getClass()));

		return (ReactiveNeo4jTransactionObject) transaction;
	}

	private static ReactiveNeo4jTransactionObject extractNeo4jTransaction(GenericReactiveTransaction status) {
		return extractNeo4jTransaction(status.getTransaction());
	}

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager transactionSynchronizationManager)
			throws TransactionException {

		ReactiveNeo4jTransactionHolder resourceHolder = (ReactiveNeo4jTransactionHolder) transactionSynchronizationManager
				.getResource(driver);
		return new ReactiveNeo4jTransactionObject(resourceHolder);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.reactive.AbstractReactiveTransactionManager#isExistingTransaction(Object)
	 */
	@Override
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {

		return extractNeo4jTransaction(transaction).hasResourceHolder();
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager transactionSynchronizationManager, Object transaction,
			TransactionDefinition transactionDefinition) throws TransactionException {

		return Mono.defer(() -> {
			ReactiveNeo4jTransactionObject transactionObject = extractNeo4jTransaction(transaction);

			TransactionConfig transactionConfig = Neo4jTransactionUtils.createTransactionConfigFrom(transactionDefinition);
			boolean readOnly = transactionDefinition.isReadOnly();

			transactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);

			return databaseSelectionProvider
					.getDatabaseSelection()
					.switchIfEmpty(Mono.just(DatabaseSelection.undecided()))
					.zipWith(
							userSelectionProvider
									.getUserSelection()
									.switchIfEmpty(Mono.just(UserSelection.connectedUser())),
							(databaseSelection, userSelection) -> new Neo4jTransactionContext(databaseSelection, userSelection, bookmarkManager.getBookmarks()))
					.map(context -> Tuples.of(context, this.driver.rxSession(Neo4jTransactionUtils.sessionConfig(readOnly, context.getBookmarks(), context.getDatabaseSelection(), context.getUserSelection()))))
					.flatMap(contextAndSession -> Mono.from(contextAndSession.getT2().beginTransaction(transactionConfig))
							.map(nativeTransaction -> new ReactiveNeo4jTransactionHolder(contextAndSession.getT1(),
									contextAndSession.getT2(), nativeTransaction)))
					.doOnNext(transactionHolder -> {
						transactionHolder.setSynchronizedWithTransaction(true);
						transactionObject.setResourceHolder(transactionHolder);
						transactionSynchronizationManager.bindResource(this.driver, transactionHolder);
					});

		}).then();
	}

	@Override
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager transactionSynchronizationManager,
			Object transaction) {

		return Mono.just(extractNeo4jTransaction(transaction)).map(r -> {
			ReactiveNeo4jTransactionHolder holder = r.getRequiredResourceHolder();
			r.setResourceHolder(null);
			return holder;
		}).flatMap(ReactiveNeo4jTransactionHolder::close)
				.then(Mono.fromRunnable(() -> transactionSynchronizationManager.unbindResource(driver)));
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		ReactiveNeo4jTransactionHolder holder = extractNeo4jTransaction(genericReactiveTransaction)
				.getRequiredResourceHolder();
		return holder.commit().doOnNext(bookmark -> bookmarkManager.updateBookmarks(holder.getBookmarks(), bookmark))
				.then();
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		ReactiveNeo4jTransactionHolder holder = extractNeo4jTransaction(genericReactiveTransaction)
				.getRequiredResourceHolder();
		return holder.rollback();
	}

	@Override
	protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager, Object transaction)
			throws TransactionException {

		return Mono.just(extractNeo4jTransaction(transaction)).doOnNext(r -> r.setResourceHolder(null))
				.then(Mono.fromSupplier(() -> synchronizationManager.unbindResource(driver)));
	}

	@Override
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager, Object transaction,
			Object suspendedResources) throws TransactionException {

		return Mono.just(extractNeo4jTransaction(transaction))
				.doOnNext(r -> r.setResourceHolder((ReactiveNeo4jTransactionHolder) suspendedResources))
				.then(Mono.fromRunnable(() -> synchronizationManager.bindResource(driver, suspendedResources)));
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.reactive.AbstractReactiveTransactionManager#doSetRollbackOnly(org.springframework.transaction.reactive.TransactionSynchronizationManager, org.springframework.transaction.reactive.GenericReactiveTransaction)
	 */
	@Override
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		return Mono.fromRunnable(() -> {
			ReactiveNeo4jTransactionObject transactionObject = extractNeo4jTransaction(genericReactiveTransaction);
			transactionObject.getRequiredResourceHolder().setRollbackOnly();
		});
	}

	static class ReactiveNeo4jTransactionObject implements SmartTransactionObject {

		private static final String RESOURCE_HOLDER_NOT_PRESENT_MESSAGE = "Neo4jConnectionHolder is required but not present. o_O";

		// The resource holder is null when the call to TransactionSynchronizationManager.getResource
		// in Neo4jTransactionManager.doGetTransaction didn't return a corresponding resource holder.
		// If it is null, there's no existing session / transaction.
		@Nullable private ReactiveNeo4jTransactionHolder resourceHolder;

		ReactiveNeo4jTransactionObject(@Nullable ReactiveNeo4jTransactionHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		/**
		 * Usually called in {@link #doBegin(TransactionSynchronizationManager, Object, TransactionDefinition)} which is
		 * called when there's no existing transaction.
		 *
		 * @param resourceHolder A newly created resource holder with a fresh drivers session,
		 */
		void setResourceHolder(@Nullable ReactiveNeo4jTransactionHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		/**
		 * @return {@literal true} if a {@link Neo4jTransactionHolder} is set.
		 */
		boolean hasResourceHolder() {
			return resourceHolder != null;
		}

		ReactiveNeo4jTransactionHolder getRequiredResourceHolder() {

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
