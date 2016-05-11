/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.transaction;

import java.util.EnumSet;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.*;

/**
 * This class is a wrapper around the OGM TransactionManager.
 *
 * @author Vince Bickers
 * @author Mark Angrish
 */
public class Neo4jTransactionManager extends AbstractPlatformTransactionManager implements ResourceTransactionManager, InitializingBean {

	private final Logger logger = LoggerFactory.getLogger(Neo4jTransactionManager.class);
	private SessionFactory sessionFactory;

	public Neo4jTransactionManager(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}


	/**
	 * Set the SessionFactory that this instance should manage transactions for.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the EntityManagerFactory that this instance should manage transactions for.
	 */
	public SessionFactory getSessionFactory() {
		return this.sessionFactory;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((Neo4jTransactionObject) transaction).hasTransaction();
	}


	@Override
	protected Object doGetTransaction() {
		Neo4jTransactionObject txObject = new Neo4jTransactionObject();
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
		if (sessionHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound Session [" + sessionHolder.getSession() +
						"] for Neo4j transaction");
			}
			txObject.setSessionHolder(sessionHolder, false);
		}

		return txObject;
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;

		try {
			if (txObject.getSessionHolder() == null ||
					txObject.getSessionHolder().isSynchronizedWithTransaction()) {
				Session session = createSessionForTransaction();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new Session [" + session + "] for Neo4j transaction");
				}
				txObject.setSessionHolder(new SessionHolder(session), true);
			}

			Session currentSession = txObject.getSessionHolder().getSession();

			Transaction rawTransaction = currentSession.beginTransaction();
			txObject.setRawTransaction(rawTransaction);

			// Bind the session holder to the thread.
			if (txObject.isNewSessionHolder()) {
				TransactionSynchronizationManager.bindResource(
						getSessionFactory(), txObject.getSessionHolder());
			}
			txObject.getSessionHolder().setSynchronizedWithTransaction(true);
		} catch (TransactionException ex) {
			closeSessionAfterFailedBegin(txObject);
			throw ex;
		} catch (Throwable ex) {
			closeSessionAfterFailedBegin(txObject);
			throw new CannotCreateTransactionException("Could not open Neo4j Session for transaction", ex);
		}
	}


	private Session createSessionForTransaction() {
		SessionFactory sessionFactory = getSessionFactory();
		return sessionFactory.openSession();
	}


	private void closeSessionAfterFailedBegin(Neo4jTransactionObject txObject) {
		if (txObject.isNewSessionHolder()) {
			Session session = txObject.getSessionHolder().getSession();
			try {
				if (isActive(session.getTransaction().status())) {
					session.getTransaction().rollback();
				}
			} catch (Throwable ex) {
				logger.debug("Could not rollback EntityManager after failed transaction begin", ex);
			} //finally {
//				session.clear();
//			}
			txObject.setSessionHolder(null, false);
		}
	}

	private static boolean isActive(Transaction.Status status) {

		return EnumSet.of(Transaction.Status.OPEN, Transaction.Status.ROLLBACK_PENDING, Transaction.Status.COMMIT_PENDING).contains(status);
	}

	@Override
	protected Object doSuspend(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;
		txObject.setSessionHolder(null, false);
		SessionHolder sessionHolder = (SessionHolder)
				TransactionSynchronizationManager.unbindResource(getSessionFactory());
		return new SuspendedResourcesHolder(sessionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		TransactionSynchronizationManager.bindResource(
				getSessionFactory(), resourcesHolder.getSessionHolder());
	}


	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try (Transaction tx = txObject.getSessionHolder().getSession().getTransaction()) {
			if (!canCommit(tx)) {
				throw new RuntimeException("Cannot commit transaction: " + tx + ", status: " + tx.status());
			}
			tx.commit();
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Rolling back Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try (Transaction tx = txObject.getSessionHolder().getSession().getTransaction()) {
			if (!canRollback(tx)) {
				throw new RuntimeException("Cannot roll back transaction: " + tx + ", status: " + tx.status());
			}
			tx.rollback();

		} finally {
			if (!txObject.isNewSessionHolder()) {
				// Clear all pending inserts/updates/deletes in the EntityManager.
				// Necessary for pre-bound EntityManagers, to avoid inconsistent state.
				txObject.getSessionHolder().getSession().clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Setting Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		status.setRollbackOnly();
	}


	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;

		// Remove the entity manager holder from the thread, if still there.
		// (Could have been removed by EntityManagerFactoryUtils in order
		// to replace it with an unsynchronized EntityManager).
		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResourceIfPossible(getSessionFactory());
		}
		txObject.getSessionHolder().clear();

		Transaction rawTransaction = txObject.getRawTransaction();

		if (rawTransaction != null && isActive(rawTransaction.status())) {
			rawTransaction.close();
		}

		// Remove the entity manager holder from the thread.
		if (txObject.isNewSessionHolder()) {
			Session em = txObject.getSessionHolder().getSession();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Neo4j Session [" + em + "] after transaction");
			}
			em.clear();
		}
		else {
			logger.debug("Not closing pre-bound JPA EntityManager after transaction");
		}
	}

	private static boolean canCommit(Transaction tx) {
		switch (tx.status()) {
			case COMMIT_PENDING:
				return true;
			case OPEN:
				return true;
			default:
				return false;
		}
	}

	private static boolean canRollback(Transaction tx) {
		switch (tx.status()) {
			case OPEN:
				return true;
			case ROLLBACK_PENDING:
				return true;
			default:
				return false;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public Object getResourceFactory() {
		return getSessionFactory();
	}


	private static class Neo4jTransactionObject {

		private SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private Transaction rawTransaction;

		public void setSessionHolder(
				SessionHolder sessionHolder, boolean newSessionHolder) {
			this.sessionHolder = sessionHolder;
			this.newSessionHolder = newSessionHolder;
		}

		public SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		public boolean isNewSessionHolder() {
			return this.newSessionHolder;
		}

		public boolean hasTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.isTransactionActive());
		}

		public void setRawTransaction(Transaction rawTransaction) {
			this.rawTransaction = rawTransaction;
			this.sessionHolder.setTransactionActive(true);
		}

		public Transaction getRawTransaction() {
			return this.rawTransaction;
		}
	}

	private static class SuspendedResourcesHolder {

		private final SessionHolder sessionHolder;


		private SuspendedResourcesHolder(SessionHolder sessionHolder) {
			this.sessionHolder = sessionHolder;
		}

		private SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

	}
}
