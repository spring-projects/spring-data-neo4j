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
package org.springframework.data.neo4j.core.transaction;

import java.io.Serial;
import java.util.Objects;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Driver;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.exceptions.RetryableException;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveUserSelectionProvider;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.data.neo4j.core.support.BookmarkManagerReference;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.reactive.AbstractReactiveTransactionManager;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.support.SmartTransactionObject;
import org.springframework.transaction.support.TransactionSynchronizationUtils;
import org.springframework.util.Assert;

/**
 * Neo4j specific implementation of an {@link AbstractReactiveTransactionManager}.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class ReactiveNeo4jTransactionManager extends AbstractReactiveTransactionManager
		implements ApplicationContextAware {

	@Serial
	private static final long serialVersionUID = 204661696798919944L;

	/**
	 * The underlying driver, which is also the synchronisation object.
	 */
	private final transient Driver driver;

	/**
	 * Database name provider.
	 */
	private final transient ReactiveDatabaseSelectionProvider databaseSelectionProvider;

	/**
	 * Provider for user impersonation.
	 */
	private final transient ReactiveUserSelectionProvider userSelectionProvider;

	private final transient BookmarkManagerReference bookmarkManager;

	/**
	 * This will create a transaction manager for the default database.
	 * @param driver a driver instance
	 */
	public ReactiveNeo4jTransactionManager(Driver driver) {

		this(with(driver));
	}

	/**
	 * This will create a transaction manager targeting whatever the database selection
	 * provider determines.
	 * @param driver a driver instance
	 * @param databaseSelectionProvider the database selection provider to determine the
	 * database in which the transactions should happen
	 */
	public ReactiveNeo4jTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

		this(with(driver).withDatabaseSelectionProvider(databaseSelectionProvider));
	}

	/**
	 * This constructor can be used to configure the bookmark manager being used. It is
	 * useful when you need to seed the bookmark manager or if you want to capture new
	 * bookmarks.
	 * @param driver a driver instance
	 * @param databaseSelectionProvider the database selection provider to determine the
	 * database in which the transactions should happen
	 * @param bookmarkManager a bookmark manager
	 */
	public ReactiveNeo4jTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider,
			Neo4jBookmarkManager bookmarkManager) {

		this(with(driver).withDatabaseSelectionProvider(databaseSelectionProvider)
			.withBookmarkManager(bookmarkManager));
	}

	private ReactiveNeo4jTransactionManager(Builder builder) {

		this.driver = builder.driver;
		this.databaseSelectionProvider = (builder.databaseSelectionProvider != null) ? builder.databaseSelectionProvider
				: ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider();
		this.userSelectionProvider = (builder.userSelectionProvider != null) ? builder.userSelectionProvider
				: ReactiveUserSelectionProvider.getDefaultSelectionProvider();
		this.bookmarkManager = new BookmarkManagerReference(Neo4jBookmarkManager::createReactive,
				builder.bookmarkManager);
	}

	/**
	 * Start building a new transaction manager for the given driver instance.
	 * @param driver a fixed driver instance.
	 * @return a builder for a transaction manager
	 */
	@API(status = API.Status.STABLE, since = "6.2")
	public static Builder with(Driver driver) {

		return new Builder(driver);
	}

	/**
	 * Retrieves a new transaction.
	 * @param driver the driver that has been used as a synchronization object.
	 * @param targetDatabase the target database
	 * @param asUser the target user
	 * @return an optional managed transaction or {@literal null} if the method hasn't
	 * been called inside an ongoing Spring transaction
	 */
	public static Mono<ReactiveTransaction> retrieveReactiveTransaction(final Driver driver,
			final DatabaseSelection targetDatabase, final UserSelection asUser) {

		// Do we have a Transaction context? Bail out early if synchronization between
		// transaction managers is not active.
		return TransactionSynchronizationManager.forCurrentTransaction()
			.filter(TransactionSynchronizationManager::isSynchronizationActive)
			.flatMap(tsm -> {
				// Get an existing holder
				ReactiveNeo4jTransactionHolder existingTxHolder = (ReactiveNeo4jTransactionHolder) tsm
					.getResource(driver);

				// And use it if there is any
				if (existingTxHolder != null) {
					return Mono.just(existingTxHolder);
				}

				// Otherwise open up a new native transaction
				return Mono.defer(() -> {

					ReactiveSession session = driver.session(ReactiveSession.class,
							Neo4jTransactionUtils.defaultSessionConfig(targetDatabase, asUser));
					return Mono.fromDirect(session.beginTransaction(Neo4jTransactionUtils
						.createTransactionConfigFrom(TransactionDefinition.withDefaults(), -1))).map(tx -> {

							ReactiveNeo4jTransactionHolder newConnectionHolder = new ReactiveNeo4jTransactionHolder(
									new Neo4jTransactionContext(targetDatabase, asUser), session, tx);
							newConnectionHolder.setSynchronizedWithTransaction(true);

							tsm.registerSynchronization(
									new ReactiveNeo4jSessionSynchronization(tsm, newConnectionHolder, driver));

							tsm.bindResource(driver, newConnectionHolder);
							return newConnectionHolder;
						});
				});
			})
			.<ReactiveTransaction>handle((connectionHolder, sink) -> {
				ReactiveTransaction transaction = connectionHolder.getTransaction(targetDatabase, asUser);
				if (transaction == null) {
					sink.error(new IllegalStateException(Neo4jTransactionUtils.formatOngoingTxInAnotherDbErrorMessage(
							connectionHolder.getDatabaseSelection(), targetDatabase,
							connectionHolder.getUserSelection(), asUser)));
					return;
				}
				sink.next(transaction);
			})
			// If not, then just don't open a transaction
			.onErrorResume(NoTransactionException.class, nte -> Mono.empty());
	}

	private static ReactiveNeo4jTransactionObject extractNeo4jTransaction(Object transaction) {

		Assert.isInstanceOf(ReactiveNeo4jTransactionObject.class, transaction,
				() -> String.format("Expected to find a %s but it turned out to be %s",
						ReactiveNeo4jTransactionObject.class, transaction.getClass()));

		return (ReactiveNeo4jTransactionObject) transaction;
	}

	private static ReactiveNeo4jTransactionObject extractNeo4jTransaction(GenericReactiveTransaction status) {
		return extractNeo4jTransaction(status.getTransaction());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.bookmarkManager.setApplicationContext(applicationContext);
	}

	@Override
	protected Object doGetTransaction(TransactionSynchronizationManager transactionSynchronizationManager)
			throws TransactionException {

		ReactiveNeo4jTransactionHolder resourceHolder = (ReactiveNeo4jTransactionHolder) transactionSynchronizationManager
			.getResource(this.driver);
		return new ReactiveNeo4jTransactionObject(resourceHolder);
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {

		return extractNeo4jTransaction(transaction).getResourceHolder() != null;
	}

	@Override
	protected Mono<Void> doBegin(TransactionSynchronizationManager transactionSynchronizationManager,
			Object transaction, TransactionDefinition transactionDefinition) throws TransactionException {

		return Mono.defer(() -> {
			ReactiveNeo4jTransactionObject transactionObject = extractNeo4jTransaction(transaction);

			TransactionConfig transactionConfig = Neo4jTransactionUtils
				.createTransactionConfigFrom(transactionDefinition, -1);
			boolean readOnly = transactionDefinition.isReadOnly();

			transactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);

			return this.databaseSelectionProvider.getDatabaseSelection()
				.switchIfEmpty(Mono.just(DatabaseSelection.undecided()))
				.zipWith(
						this.userSelectionProvider.getUserSelection()
							.switchIfEmpty(Mono.just(UserSelection.connectedUser())),
						(databaseSelection, userSelection) -> new Neo4jTransactionContext(databaseSelection,
								userSelection, this.bookmarkManager.resolve().getBookmarks()))
				.map(context -> Tuples.of(context,
						this.driver.session(ReactiveSession.class,
								Neo4jTransactionUtils.sessionConfig(readOnly, context.getBookmarks(),
										context.getDatabaseSelection(), context.getUserSelection()))))
				.flatMap(contextAndSession -> Mono
					.fromDirect(contextAndSession.getT2().beginTransaction(transactionConfig))
					.single()
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
		})
			.flatMap(ReactiveNeo4jTransactionHolder::close)
			.then(Mono.fromRunnable(() -> transactionSynchronizationManager.unbindResource(this.driver)));
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		ReactiveNeo4jTransactionHolder holder = extractNeo4jTransaction(genericReactiveTransaction)
			.getRequiredResourceHolder();
		return holder.commit()
			.doOnNext(bookmark -> this.bookmarkManager.resolve().updateBookmarks(holder.getBookmarks(), bookmark))
			.onErrorMap(e -> e instanceof RetryableException,
					ex -> new TransactionSystemException(
							Objects.requireNonNullElse(ex.getMessage(), "Caught a retryable exception"), ex))
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

		return Mono.just(extractNeo4jTransaction(transaction))
			.doOnNext(r -> r.setResourceHolder(null))
			.then(Mono.fromSupplier(() -> synchronizationManager.unbindResource(this.driver)));
	}

	@Override
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager,
			@Nullable Object transaction, Object suspendedResources) throws TransactionException {

		return Mono.just(extractNeo4jTransaction(Objects.requireNonNull(transaction)))
			.doOnNext(r -> r.setResourceHolder((ReactiveNeo4jTransactionHolder) suspendedResources))
			.then(Mono.fromRunnable(() -> synchronizationManager.bindResource(this.driver, suspendedResources)));
	}

	@Override
	protected Mono<Void> doSetRollbackOnly(TransactionSynchronizationManager synchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		return Mono.fromRunnable(() -> {
			ReactiveNeo4jTransactionObject transactionObject = extractNeo4jTransaction(genericReactiveTransaction);
			transactionObject.getRequiredResourceHolder().setRollbackOnly();
		});
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
		 * Configures the database selection provider. Make sure to use the same instance
		 * as for a possible
		 * {@link org.springframework.data.neo4j.core.ReactiveNeo4jClient}. During
		 * runtime, it will be checked if a call is made for the same database when
		 * happening in a managed transaction.
		 * @param databaseSelectionProvider the database selection provider
		 * @return the builder
		 */
		public Builder withDatabaseSelectionProvider(
				@Nullable ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
			this.databaseSelectionProvider = databaseSelectionProvider;
			return this;
		}

		/**
		 * Configures a provider for impersonated users. Make sure to use the same
		 * instance as for a possible
		 * {@link org.springframework.data.neo4j.core.ReactiveNeo4jClient}. During
		 * runtime, it will be checked if a call is made for the same user when happening
		 * in a managed transaction.
		 * @param userSelectionProvider the provider for impersonated users
		 * @return the builder
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

	static class ReactiveNeo4jTransactionObject implements SmartTransactionObject {

		private static final String RESOURCE_HOLDER_NOT_PRESENT_MESSAGE = "Neo4jConnectionHolder is required but not present. o_O";

		// The resource holder is null when the call to
		// TransactionSynchronizationManager.getResource
		// in Neo4jTransactionManager.doGetTransaction didn't return a corresponding
		// resource holder.
		// If it is null, there's no existing session / transaction.
		@Nullable
		private ReactiveNeo4jTransactionHolder resourceHolder;

		ReactiveNeo4jTransactionObject(@Nullable ReactiveNeo4jTransactionHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		ReactiveNeo4jTransactionHolder getRequiredResourceHolder() {

			return Objects.requireNonNull(this.resourceHolder, RESOURCE_HOLDER_NOT_PRESENT_MESSAGE);
		}

		@Nullable ReactiveNeo4jTransactionHolder getResourceHolder() {
			return this.resourceHolder;
		}

		/**
		 * Usually called in
		 * {@link #doBegin(TransactionSynchronizationManager, Object, TransactionDefinition)}
		 * which is called when there's no existing transaction.
		 * @param resourceHolder a newly created resource holder with a fresh drivers'
		 * session,
		 */
		void setResourceHolder(@Nullable ReactiveNeo4jTransactionHolder resourceHolder) {
			this.resourceHolder = resourceHolder;
		}

		@Override
		public boolean isRollbackOnly() {
			return this.resourceHolder != null && this.resourceHolder.isRollbackOnly();
		}

		@Override
		public void flush() {

			TransactionSynchronizationUtils.triggerFlush();
		}

	}

}
