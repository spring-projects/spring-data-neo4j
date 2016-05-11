package org.springframework.data.neo4j.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.core.Ordered;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Created by markangrish on 14/05/2016.
 */
public class SessionFactoryUtils {

	private static final Log logger = LogFactory.getLog(SessionFactoryUtils.class);


	public static void releaseSession(Session session, SessionFactory sessionFactory) {
		if (session == null) {
			return;
		}
	}

	public static Session getSession(SessionFactory sessionFactory, boolean allowCreate) throws IllegalStateException {

		Assert.notNull(sessionFactory, "No SessionFactory specified");

		SessionHolder sessionHolder =
				(SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
		if (sessionHolder != null) {
			if (!sessionHolder.isSynchronizedWithTransaction() &&
					TransactionSynchronizationManager.isSynchronizationActive()) {
				sessionHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new SessionSynchronization(sessionHolder, sessionFactory, false));
			}
			return sessionHolder.getSession();
		}

		if (!allowCreate && !TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("No Neo4j Session bound to thread, " +
					"and configuration does not allow creation of non-transactional one here");
		}

		logger.debug("Opening Neo4j Session");
		Session session = sessionFactory.openSession();

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			logger.debug("Registering transaction synchronization for Neo4j Session");
			// Use same PersistenceManager for further Neo4j actions within the transaction.
			// Thread object will get removed by synchronization at transaction completion.
			sessionHolder = new SessionHolder(session);
			sessionHolder.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.registerSynchronization(
					new SessionSynchronization(sessionHolder, sessionFactory, true));
			TransactionSynchronizationManager.bindResource(sessionFactory, sessionHolder);
		}

		return session;
	}


	private static class SessionSynchronization
			extends ResourceHolderSynchronization<SessionHolder, SessionFactory>
			implements Ordered {

		private final boolean newSession;

		public SessionSynchronization(
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

		@Override
		protected void releaseResource(SessionHolder resourceHolder, SessionFactory resourceKey) {
			releaseSession(resourceHolder.getSession(), resourceKey);
		}
	}
}
