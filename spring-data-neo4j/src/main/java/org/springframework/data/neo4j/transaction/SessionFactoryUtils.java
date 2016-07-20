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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.exception.InvalidDepthException;
import org.neo4j.ogm.exception.NotFoundException;
import org.neo4j.ogm.exception.ResultProcessingException;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.core.Ordered;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class featuring methods for Neo4j OGM Session handling,
 * allowing for reuse of Session instances within transactions.
 * Also provides support for exception translation.
 *
 * <p>Mainly intended for internal use within the framework.
 *
 * @author Mark Angrish
 */
public class SessionFactoryUtils {

	private static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);

	private static ThreadLocal<Session> sessions = new ThreadLocal<>();

	public static void closeSession() {
		sessions.remove();
	}

	public static Session getSession(SessionFactory sessionFactory) throws IllegalStateException {

		Assert.notNull(sessionFactory, "No SessionFactory specified");

		SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		if (sessionHolder != null) {
			if (!sessionHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				sessionHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new SessionSynchronization(sessionHolder, sessionFactory, false));
			}
			return sessionHolder.getSession();
		}

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("No Neo4j Session bound to thread, " +
					"and configuration does not allow creation of non-transactional one here");
		}

		Session session = sessions.get();

		logger.debug("Found existing Neo4j Session [" + session + "]");

		if (session == null) {
			session = sessionFactory.openSession();
			logger.debug("Opening Neo4j Session [" + session + "]");
			sessions.set(session);
		}


		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for Neo4j Session");
			// Use same Session for further Neo4j actions within the transaction.
			// Thread object will get removed by synchronization at transaction completion.
			sessionHolder = new SessionHolder(session);
			sessionHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(
					new SessionSynchronization(sessionHolder, sessionFactory, true));
			TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);
		}

		return session;
	}

	/**
	 * Convert the given runtime exception to an appropriate exception from the
	 * {@code org.springframework.dao} hierarchy.
	 * Return null if no translation is appropriate: any other exception may
	 * have resulted from user code, and should not be translated.
	 * @param ex runtime exception that occurred
	 * @return the corresponding DataAccessException instance,
	 * or {@code null} if the exception should not be translated
	 */
	public static DataAccessException convertOgmAccessException(RuntimeException ex) {

		if (ex instanceof IllegalStateException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof IllegalArgumentException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof NotFoundException) {
			return new DataRetrievalFailureException(ex.getMessage(), ex);
		}
		if (ex instanceof InvalidDepthException) {
			return new InvalidDataAccessApiUsageException(ex.getMessage(), ex);
		}
		if (ex instanceof ResultProcessingException) {
			return new DataRetrievalFailureException(ex.getMessage(), ex);
		}

		// If we get here, we have an exception that resulted from user code,
		// rather than the persistence provider, so we return null to indicate
		// that translation should not occur.
		return null;
	}



	private static class SessionSynchronization
			extends ResourceHolderSynchronization<SessionHolder, SessionFactory>
			implements Ordered {

		private final boolean newSession;

		SessionSynchronization(
				SessionHolder sessionHolder, SessionFactory sessionFactory, boolean newSession) {
			super(sessionHolder, sessionFactory);
			this.newSession = newSession;
		}

		@Override
		public int getOrder() {
			return 900;
		}

		@Override
		public void flushResource(SessionHolder resourceHolder) {
//			resourceHolder.getSession().clear();
		}

		@Override
		protected boolean shouldUnbindAtCompletion() {
			return this.newSession;
		}

		@Override
		protected boolean shouldReleaseAfterCompletion(SessionHolder resourceHolder) {
//			return !resourceHolder.getSession().isClosed();
			return false;
		}
	}
}
