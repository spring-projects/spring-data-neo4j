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

import static java.util.Collections.*;

import java.util.Collection;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.bookmark.BookmarkInfo;
import org.springframework.data.neo4j.bookmark.BookmarkManager;
import org.springframework.data.neo4j.bookmark.BookmarkSupport;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.ResourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * {@link PlatformTransactionManager} implementation for a single Neo4j OGM {@link SessionFactory}. Binds a Neo4j OGM
 * Session from the specified factory to the thread, potentially allowing for one thread-bound Session per factory.
 * {@link SharedSessionCreator} is aware of thread-bound session and participates in such transactions automatically. It
 * is required for Neo4j OGM access code supporting this transaction management mechanism.
 * <p>
 * This transaction manager is appropriate for applications that use a single Neo4j OGM SessionFactory for transactional
 * data access. JTA (usually through {@link org.springframework.transaction.jta.JtaTransactionManager}) has not been
 * tested or considered at the moment.
 * <p>
 * This transaction manager does not support nested transactions or requires new propagation.
 *
 * @author Mark Angrish
 * @see #setSessionFactory
 */
public class Neo4jTransactionManager extends AbstractPlatformTransactionManager
		implements ResourceTransactionManager, BeanFactoryAware, InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(Neo4jTransactionManager.class);

	private SessionFactory sessionFactory;
	private BookmarkManager bookmarkManager;

	/**
	 * Create a new Neo4jTransactionManager instance.
	 * <p>
	 * An SessionFactory has to be set to be able to use it.
	 *
	 * @see #setSessionFactory(SessionFactory)
	 */
	public Neo4jTransactionManager() {
		setTransactionSynchronization(SYNCHRONIZATION_ON_ACTUAL_TRANSACTION);
	}

	/**
	 * Create a new Neo4jTransactionManager instance.
	 *
	 * @param sessionFactory SessionFactory to manage transactions for
	 */
	public Neo4jTransactionManager(SessionFactory sessionFactory) {
		this();
		this.sessionFactory = sessionFactory;
	}

	/**
	 * Set the SessionFactory that this instance should manage transactions for.
	 * <p>
	 * By default, a default SessionFactory will be retrieved by finding a single unique bean of type SessionFactory in
	 * the containing BeanFactory.
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

	/**
	 * Retrieves a default SessionFactory bean.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (getSessionFactory() == null) {
			setSessionFactory(beanFactory.getBean(SessionFactory.class));
		}
		try {
			bookmarkManager = beanFactory.getBean(BookmarkManager.class);
			logger.debug("Found BookmarkManager {}", bookmarkManager);
		} catch (NoSuchBeanDefinitionException e) {
			logger.debug("No BookmarkManager bean found, bookmark management not enabled");
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

	@Override
	protected Object doGetTransaction() {
		Neo4jTransactionObject txObject = new Neo4jTransactionObject();

		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(getSessionFactory());
		if (sessionHolder != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Found thread-bound Session [" + sessionHolder.getSession() + "] for Neo4j OGM transaction");
			}
			txObject.setSessionHolder(sessionHolder, false);
		}
		return txObject;
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((Neo4jTransactionObject) transaction).hasTransaction();
	}

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;

		try {
			if (txObject.getSessionHolder() == null || txObject.getSessionHolder().isSynchronizedWithTransaction()) {
				Session session = sessionFactory.openSession();
				if (logger.isDebugEnabled()) {
					logger.debug("Opened new Session [" + session + "] for Neo4j OGM transaction");
				}
				txObject.setSessionHolder(new SessionHolder(session), true);
			}

			Session session = txObject.getSessionHolder().getSession();

			if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
				// We should set a specific isolation level but are not allowed to...
				throw new InvalidIsolationLevelException(
						"Neo4jTransactionManager is not allowed to support custom isolation levels.");
			}

			if (definition.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
				throw new IllegalTransactionStateException("Neo4jTransactionManager only supports 'required' propagation.");
			}

			Transaction.Type type = getTransactionType(definition, txObject);
			Transaction transactionData = session.beginTransaction(type, getBookmarks());

			txObject.setTransactionData(transactionData);
			if (logger.isDebugEnabled()) {
				logger.debug("Beginning Transaction [" + transactionData + "] on Session [" + session + "]");
			}

			// Bind the session holder to the thread.
			if (txObject.isNewSessionHolder()) {
				TransactionSynchronizationManager.bindResource(getSessionFactory(), txObject.getSessionHolder());
			}

			if (definition.isReadOnly()) {
				TransactionSynchronizationManager.setCurrentTransactionReadOnly(true);
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

	private Transaction.Type getTransactionType(TransactionDefinition definition, Neo4jTransactionObject txObject) {
		Transaction.Type type;
		if (definition.isReadOnly() && txObject.isNewSessionHolder()) {
			type = Transaction.Type.READ_ONLY;
		} else if (txObject.transactionData != null) {
			type = txObject.transactionData.type();
		} else {
			type = Transaction.Type.READ_WRITE;
		}
		return type;
	}

	private Iterable<String> getBookmarks() {
		BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
		if (bookmarkInfo != null && bookmarkInfo.shouldUseBookmark()) {
			if (bookmarkManager != null) {
				Collection<String> bookmarks = bookmarkManager.getBookmarks();
				bookmarkInfo.setBookmarks(bookmarks);
				return bookmarks;

			} else {
				throw new IllegalStateException("Configured @UseBookmark but no BookmarkManager bean found.");
			}
		} else {
			return emptySet();
		}
	}

	/**
	 * Close the current transaction's Session. Called after a transaction begin attempt failed.
	 *
	 * @param txObject the current transaction
	 */
	private void closeSessionAfterFailedBegin(Neo4jTransactionObject txObject) {
		if (txObject.isNewSessionHolder()) {
			Session session = txObject.getSessionHolder().getSession();
			try {
				if (session.getTransaction() != null) {
					session.getTransaction().rollback();
				}
			} catch (Throwable ex) {
				logger.debug("Could not rollback Session after failed transaction begin", ex);
			} finally {
				// close session.
				SessionFactoryUtils.closeSession(session);
			}
			txObject.setSessionHolder(null, false);
		}
	}

	@Override
	protected Object doSuspend(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;
		txObject.setSessionHolder(null, false);
		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(getSessionFactory());
		return new SuspendedResourcesHolder(sessionHolder);
	}

	@Override
	protected void doResume(Object transaction, Object suspendedResources) {
		SuspendedResourcesHolder resourcesHolder = (SuspendedResourcesHolder) suspendedResources;
		if (TransactionSynchronizationManager.hasResource(getSessionFactory())) {
			// From non-transactional code running in active transaction synchronization
			// -> can be safely removed, will be closed on transaction completion.
			TransactionSynchronizationManager.unbindResource(getSessionFactory());
		}
		TransactionSynchronizationManager.bindResource(getSessionFactory(), resourcesHolder.getSessionHolder());
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {

		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		Session session = txObject.getSessionHolder().getSession();

		try (Transaction tx = session.getTransaction()) {

			if (status.isDebug()) {
				logger.debug("Committing Neo4j OGM transaction [" + tx + "] on Session [" + session + "]");
			}

			if (tx != null) {
				tx.commit();

				if (bookmarkManager != null) {
					String lastBookmark = session.getLastBookmark();

					BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
					Collection<String> bookmarks;
					if (bookmarkInfo != null && bookmarkInfo.getBookmarks() != null) {
						bookmarks = bookmarkInfo.getBookmarks();
					} else {
						bookmarks = emptySet();
					}
					logger.debug("Found bookmarks {}, replacing with {}", bookmarks, lastBookmark);
					bookmarkManager.storeBookmark(lastBookmark, bookmarks);
				}
			}
		} catch (RuntimeException ex) {
			DataAccessException dae = SessionFactoryUtils.convertOgmAccessException(ex);
			throw (dae != null ? dae : ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		Session session = txObject.getSessionHolder().getSession();

		try (Transaction tx = session.getTransaction()) {

			if (status.isDebug()) {
				logger.debug("Rolling back Neo4j transaction [" + tx + "] on Session [" + session + "]");
			}

			if (tx != null) {
				tx.rollback();
			}
		} catch (RuntimeException ex) {
			DataAccessException dae = SessionFactoryUtils.convertOgmAccessException(ex);
			throw (dae != null ? dae : ex);
		} finally {
			if (!txObject.isNewSessionHolder()) {
				// Clear all pending inserts/updates/deletes in the Session.
				session.clear();
			}
		}
	}

	@Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) status.getTransaction();
		if (status.isDebug()) {
			logger.debug(
					"Setting Neo4j OGM transaction on Session [" + txObject.getSessionHolder().getSession() + "] rollback-only");
		}
		status.setRollbackOnly();
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		Neo4jTransactionObject txObject = (Neo4jTransactionObject) transaction;

		// Remove the session holder from the thread, if still there.
		if (txObject.isNewSessionHolder()) {
			TransactionSynchronizationManager.unbindResourceIfPossible(getSessionFactory());
		}

		Transaction rawTransaction = txObject.getTransactionData();

		if (rawTransaction != null && rawTransaction.status().equals(Transaction.Status.OPEN)) {
			rawTransaction.close();
		}

		// Remove the session holder from the thread.
		if (txObject.isNewSessionHolder()) {
			Session session = txObject.getSessionHolder().getSession();
			if (logger.isDebugEnabled()) {
				logger.debug("Closing Neo4j Session [" + session + "] after transaction");
			}
			// close session.
			SessionFactoryUtils.closeSession(session);
		} else {
			logger.debug("Not closing pre-bound Neo4j Session after transaction");
		}

		txObject.getSessionHolder().clear();
	}

	/**
	 * Neo4j OGM transaction object, representing a SessionHolder. Used as transaction object by Neo4jTransactionManager.
	 */
	private static class Neo4jTransactionObject {

		private SessionHolder sessionHolder;

		private boolean newSessionHolder;

		private Transaction transactionData;

		void setSessionHolder(SessionHolder sessionHolder, boolean newSessionHolder) {
			this.sessionHolder = sessionHolder;
			this.newSessionHolder = newSessionHolder;
		}

		SessionHolder getSessionHolder() {
			return this.sessionHolder;
		}

		boolean isNewSessionHolder() {
			return this.newSessionHolder;
		}

		boolean hasTransaction() {
			return (this.sessionHolder != null && this.sessionHolder.isTransactionActive());
		}

		void setTransactionData(Transaction rawTransaction) {
			this.transactionData = rawTransaction;
			this.sessionHolder.setTransactionActive(true);
		}

		Transaction getTransactionData() {
			return this.transactionData;
		}
	}

	/**
	 * Holder for suspended resources. Used internally by {@code doSuspend} and {@code doResume}.
	 */
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
