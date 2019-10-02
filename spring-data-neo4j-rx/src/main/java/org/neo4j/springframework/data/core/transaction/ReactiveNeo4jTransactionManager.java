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

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
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

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public class ReactiveNeo4jTransactionManager extends AbstractReactiveTransactionManager {

	/**
	 * The underlying driver, which is also the synchronisation object.
	 */
	private final Driver driver;
	/**
	 * The name of the target database.
	 */
	private final String databaseName;

	public ReactiveNeo4jTransactionManager(Driver driver) {
		this(driver, null);
	}

	public ReactiveNeo4jTransactionManager(Driver driver, String databaseName) {
		this.driver = driver;
		this.databaseName = databaseName;
	}

	public static Mono<RxTransaction> retrieveReactiveTransaction(final Driver driver, final String targetDatabase) {

		return TransactionSynchronizationManager.forCurrentTransaction() // Do we have a Transaction context?
			// Bail out early if synchronization between transaction managers is not active
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
					RxSession session = driver.rxSession(defaultSessionConfig(targetDatabase));
					return Mono.from(session.beginTransaction(TransactionConfig.empty())).map(tx -> {

						ReactiveNeo4jTransactionHolder newConnectionHolder = new ReactiveNeo4jTransactionHolder(
							targetDatabase, session, tx);
						newConnectionHolder.setSynchronizedWithTransaction(true);

						tsm.registerSynchronization(
							new ReactiveNeo4jSessionSynchronization(tsm, newConnectionHolder, driver));

						tsm.bindResource(driver, newConnectionHolder);
						return newConnectionHolder;
					});
				});
			})
			.map(connectionHolder -> {
					RxTransaction transaction = connectionHolder.getTransaction(targetDatabase);
					if (transaction == null) {
						throw new IllegalStateException(
							formatOngoingTxInAnotherDbErrorMessage(connectionHolder.getDatabaseName(), targetDatabase));
					}
					return transaction;
				}
			)
			// If not, than just don't open a transaction
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

			TransactionConfig transactionConfig = createTransactionConfigFrom(transactionDefinition);
			boolean readOnly = transactionDefinition.isReadOnly();

			transactionSynchronizationManager.setCurrentTransactionReadOnly(readOnly);

			List<Bookmark> bookmarks = Collections.emptyList(); // TODO Bookmarksupport;
			RxSession session = this.driver.rxSession(sessionConfig(readOnly, bookmarks, databaseName));

			return Mono.from(session.beginTransaction(transactionConfig)).doOnNext(nativeTransaction -> {

				ReactiveNeo4jTransactionHolder transactionHolder =
					new ReactiveNeo4jTransactionHolder(databaseName, session, nativeTransaction);
				transactionHolder.setSynchronizedWithTransaction(true);
				transactionObject.setResourceHolder(transactionHolder);
				transactionSynchronizationManager.bindResource(this.driver, transactionHolder);
			});

		}).then();
	}

	@Override
	protected Mono<Void> doCleanupAfterCompletion(TransactionSynchronizationManager transactionSynchronizationManager,
		Object transaction) {

		return Mono
			.just(extractNeo4jTransaction(transaction))
			.map(r -> {
				ReactiveNeo4jTransactionHolder holder = r.getRequiredResourceHolder();
				r.setResourceHolder(null);
				return holder;
			})
			.flatMap(ReactiveNeo4jTransactionHolder::close)
			.then(Mono.fromRunnable(() -> transactionSynchronizationManager.unbindResource(driver)));
	}

	@Override
	protected Mono<Void> doCommit(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		ReactiveNeo4jTransactionHolder holder = extractNeo4jTransaction(genericReactiveTransaction)
			.getRequiredResourceHolder();
		return holder.commit();
	}

	@Override
	protected Mono<Void> doRollback(TransactionSynchronizationManager transactionSynchronizationManager,
			GenericReactiveTransaction genericReactiveTransaction) throws TransactionException {

		ReactiveNeo4jTransactionHolder holder = extractNeo4jTransaction(genericReactiveTransaction)
			.getRequiredResourceHolder();
		return holder.rollback();
	}

	@Override
	protected Mono<Object> doSuspend(TransactionSynchronizationManager synchronizationManager, Object transaction) throws TransactionException {

		return Mono
			.just(extractNeo4jTransaction(transaction))
			.doOnNext(r -> r.setResourceHolder(null))
			.then(Mono.fromSupplier(() -> synchronizationManager.unbindResource(driver)));
	}

	@Override
	protected Mono<Void> doResume(TransactionSynchronizationManager synchronizationManager, Object transaction, Object suspendedResources) throws TransactionException {

		return Mono
			.just(extractNeo4jTransaction(transaction))
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
