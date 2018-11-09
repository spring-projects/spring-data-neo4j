/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import static org.neo4j.ogm.transaction.Transaction.Status.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.mapping.MetaDataProvider;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

/**
 * Delegate for creating a shareable Neo4j OGM {@link Session} reference for a given {@link SessionFactory}.
 * <p>
 * A shared Session will behave just like a Session fetched from an application SessionFactory. It will delegate all
 * calls to the current transactional Session, if any; otherwise it will fall back to a newly created Session per
 * operation.
 *
 * @author Mark Angrish
 * @author Michael J. Simons
 * @see Neo4jTransactionManager
 */
public class SharedSessionCreator {

	private static final Set<String> TRANSACTION_REQUIRING_METHODS;

	static {
		Set<String> tmp = new HashSet<>();
		tmp.add("deleteAll");
		tmp.add("save");
		tmp.add("delete");
		tmp.add("purgeDatabase");

		TRANSACTION_REQUIRING_METHODS = Collections.unmodifiableSet(tmp);
	}

	/**
	 * Create a transactional Session proxy for the given SessionFactory.
	 *
	 * @param sessionFactory SessionFactory to obtain Sessions from as needed {@code createSession} call (may be
	 *          {@code null}) Session. Allows the addition or specification of proprietary interfaces.
	 * @return a shareable transactional Session proxy
	 */
	public static Session createSharedSession(SessionFactory sessionFactory) {
		return (Session) Proxy.newProxyInstance(SharedSessionCreator.class.getClassLoader(),
				new Class<?>[] { Session.class, MetaDataProvider.class }, new SharedSessionInvocationHandler(sessionFactory));
	}

	/**
	 * Invocation handler that delegates all calls to the current transactional Session, if any; else, it will fall back
	 * to a newly created Session per operation.
	 */
	private static class SharedSessionInvocationHandler implements InvocationHandler {

		private final Logger logger = LoggerFactory.getLogger(getClass());

		private final SessionFactory sessionFactory;

		public SharedSessionInvocationHandler(SessionFactory sessionFactory) {
			this.sessionFactory = sessionFactory;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			String methodName = method.getName();
			switch (methodName) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of Session proxy.
					return hashCode();
				case "toString":
					// Deliver toString without touching a target Session.
					return "Shared Session proxy for target factory [" + sessionFactory + "]";
				case "getMetaData":
					return this.sessionFactory.metaData();
				case "beginTransaction":
					throw new IllegalStateException(
							"Not allowed to create transaction on shared Session - " + "use Spring transactions instead");
				default:
					Function<Session, Object> methodCall = targetSession -> ReflectionUtils.invokeMethod(method, targetSession,
							args);
					return invokeInTransaction(methodName, methodCall);
			}
		}

		private Object invokeInTransaction(String methodName, Function<Session, Object> methodCall) {

			// Determine current Session: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			Session targetSession = SessionFactoryUtils.getSession(this.sessionFactory);

			if (TRANSACTION_REQUIRING_METHODS.contains(methodName)) {
				if (targetSession == null
						|| (!TransactionSynchronizationManager.isActualTransactionActive() && targetSession.getTransaction() != null
								&& EnumSet.of(CLOSED, COMMITTED, ROLLEDBACK).contains(targetSession.getTransaction().status()))) {
					throw new IllegalStateException("No Session with actual transaction available "
							+ "for current thread - cannot reliably process '" + methodName + "' call");
				}
			}

			// Regular Session operations.
			boolean isNewSession = false;
			if (targetSession == null) {
				logger.debug("Creating new Session for shared Session invocation");
				targetSession = this.sessionFactory.openSession();
				isNewSession = true;
			}

			// Invoke method on current Session.
			try {
				return methodCall.apply(targetSession);
			} finally {
				if (isNewSession) {
					SessionFactoryUtils.closeSession(targetSession);
				}
			}
		}
	}
}
