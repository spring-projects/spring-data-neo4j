/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.transaction;

import static org.neo4j.ogm.transaction.Transaction.Status.*;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
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

		private final Method queryMethod;

		public SharedSessionInvocationHandler(SessionFactory sessionFactory) {
			this.sessionFactory = sessionFactory;
			this.queryMethod = ReflectionUtils
					.findMethod(Session.class, "query", String.class, Map.class, boolean.class);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
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
							"Not allowed to create transaction on shared Session - "
									+ "use Spring transactions instead");
				default:
					Function<Session, Object> methodCall;
					if (isGenericQueryMethod(method)) {
						Object[] newArgs = new Object[args.length + 1];
						System.arraycopy(args, 0, newArgs, 0, args.length);
						newArgs[newArgs.length - 1] = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
						methodCall = targetSession -> ReflectionUtils.invokeMethod(queryMethod, targetSession, newArgs);
					} else {
						methodCall = targetSession -> ReflectionUtils.invokeMethod(method, targetSession, args);
					}
					return invokeInTransaction(methodName, methodCall);
			}
		}

		private static boolean isGenericQueryMethod(Method method) {

			Parameter[] parameters = method.getParameters();
			return "query".equals(method.getName()) && method.getParameterCount() == 2 &&
					parameters[0].getType() == String.class && parameters[1].getType() == Map.class;
		}

		private Object invokeInTransaction(String methodName, Function<Session, Object> methodCall) {

			// Determine current Session: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			Session targetSession = SessionFactoryUtils.getSession(this.sessionFactory);

			if (TRANSACTION_REQUIRING_METHODS.contains(methodName)) {
				if (targetSession == null
						|| (!TransactionSynchronizationManager.isActualTransactionActive()
						&& targetSession.getTransaction() != null
						&& EnumSet.of(CLOSED, COMMITTED, ROLLEDBACK)
						.contains(targetSession.getTransaction().status()))) {
					throw new IllegalStateException("No Session with actual transaction available "
							+ "for current thread - cannot reliably process '" + methodName + "' call");
				}
			}

			// Regular Session operations.
			if (targetSession == null) {
				logger.debug("Creating new Session for shared Session invocation");
				targetSession = this.sessionFactory.openSession();
			}

			// Invoke method on current Session.
			try {
				return methodCall.apply(targetSession);
			} finally {
			}
		}
	}
}
