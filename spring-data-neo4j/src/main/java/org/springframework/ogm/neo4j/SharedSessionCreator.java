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

package org.springframework.ogm.neo4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/**
 * Delegate for creating a shareable Neo4j OGM {@link Session}
 * reference for a given {@link SessionFactory}.
 *
 * <p>A shared Session will behave just like a Session fetched from
 * an application SessionFactory.
 * It will delegate all calls to the current transactional Session, if any;
 * otherwise it will fall back to a newly created Session per operation.
 *
 *
 * @author Mark Angrish
 * @see Neo4jTransactionManager
 */
public class SharedSessionCreator {

	/**
	 * Create a transactional Session proxy for the given SessionFactory.
	 *
	 * @param sessionFactory SessionFactory to obtain Sessions from as needed
	 * {@code createSession} call (may be {@code null})
	 * Session. Allows the addition or specification of proprietary interfaces.
	 * @return a shareable transactional Session proxy
	 */
	public static Session createSharedSession(SessionFactory sessionFactory) {

		return (Session) Proxy.newProxyInstance(
				SharedSessionCreator.class.getClassLoader(),
				new Class<?>[]{Session.class}, new SharedSessionInvocationHandler(sessionFactory));
	}


	/**
	 * Invocation handler that delegates all calls to the current
	 * transactional Session, if any; else, it will fall back
	 * to a newly created Session per operation.
	 */
	private static class SharedSessionInvocationHandler implements InvocationHandler {

		private final SessionFactory targetFactory;

		public SharedSessionInvocationHandler(SessionFactory target) {
			this.targetFactory = target;
		}


		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on Session interface coming in...

			if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0]);
			} else if (method.getName().equals("hashCode")) {
				// Use hashCode of PersistenceManager proxy.
				return System.identityHashCode(proxy);
			} else if (method.getName().equals("toString")) {
				// Deliver toString without touching a target Session.
				return "Spring Session proxy for target factory [" + targetFactory + "]";
			}  else if (method.getName().equals("beginTransaction")) {
				throw new IllegalStateException(
						"Not allowed to create transaction on shared Session - " +
								"use Spring transactions instead");
			}

			// Determine current Session: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			Session target = SessionFactoryUtils.getSession(this.targetFactory);

			// Regular Session operations.
			if (target == null) {
				target = this.targetFactory.openSession();
			}

			// Invoke method on current Session.
			try {
				return method.invoke(target, args);
			} catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}finally {
				//TODO: (mangrish) check if this is a new session and only then close.
				SessionFactoryUtils.closeSession();
			}
		}
	}
}
