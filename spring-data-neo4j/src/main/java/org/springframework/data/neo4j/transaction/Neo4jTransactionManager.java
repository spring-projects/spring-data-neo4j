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

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.template.Neo4jOgmExceptionTranslator;
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
		setTransactionSynchronization(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	/**
	 * Set the SessionFactory that this instance should manage transactions for.
	 */
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Return the SessionFactory that this instance should manage transactions for.
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

			Session session = txObject.getSessionHolder().getSession();

			Transaction transactionData = session.beginTransaction();
			txObject.setTransactionData(transactionData);

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
				if (session.getTransaction().status().equals(Transaction.Status.OPEN)) {
					session.getTransaction().rollback();
				}
			} catch (Throwable ex) {
				logger.debug("Could not rollback Session after failed transaction begin", ex);
			} finally {
				// do any cleanup here.
			}
			txObject.setSessionHolder(null, false);
		}
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
	protected void doCommit(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug("Committing Neo4j transaction on Session [" +
					txObject.getSessionHolder().getSession() + "]");
		}

		try (Transaction tx = txObject.getSessionHolder().getSession().getTransaction()) {
			tx.commit();
		}
		catch (RuntimeException ex) {
			throw DataAccessUtils.translateIfNecessary(ex, new PersistenceExceptionTranslator() {
				@Override
				public DataAccessException translateExceptionIfPossible(RuntimeException e) {
					throw Neo4jOgmExceptionTranslator.translateExceptionIfPossible(e);
				}
			});
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
			if (tx.status().equals(Transaction.Status.OPEN)) {
				tx.rollback();
			}
		}
		finally {
			if (!txObject.isNewSessionHolder()) {
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

		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResourceIfPossible(getSessionFactory());
		}
		txObject.getSessionHolder().clear();

		Transaction rawTransaction = txObject.getTransactionData();

		if (rawTransaction != null && rawTransaction.status().equals(Transaction.Status.OPEN)) {
			rawTransaction.close();
		}

		if (txObject.isNewSessionHolder()) {
			Session session = txObject.getSessionHolder().getSession();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Neo4j Session [" + session + "] after transaction");
			}
//			session.clear();
		}
		else {
			logger.debug("Not closing pre-bound Neo4j Session after transaction");
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (getSessionFactory() == null) {
			throw new IllegalArgumentException("'sessionFactory' is required");
		}
	}

	@Override
	public Object getResourceFactory() {
		return getSessionFactory();
	}


	private static class Neo4jTransactionObject {

		private SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private Transaction transactionData;

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

		public void setTransactionData(Transaction rawTransaction) {
			this.transactionData = rawTransaction;
			this.sessionHolder.setTransactionActive(true);
		}

		public Transaction getTransactionData() {
			return this.transactionData;
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
