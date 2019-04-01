/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.support;

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ReflectionUtils;

/**
 * @author Michael J. Simons
 */
@Slf4j
final class SharedNodeManagerCreator {

	private static final Set<String> TRANSACTION_REQUIRING_METHODS;

	static {
		Set<String> tmp = new HashSet<>();

		tmp.add("save");
		tmp.add("delete");

		TRANSACTION_REQUIRING_METHODS = Collections.unmodifiableSet(tmp);
	}

	public static NodeManager createSharedNodeManager(NodeManagerFactory nodeManagerFactory) {

		return (NodeManager) Proxy.newProxyInstance(SharedNodeManagerCreator.class.getClassLoader(),
			new Class<?>[] { NodeManager.class }, new SharedNodeManagerInvocationHandler(nodeManagerFactory));
	}

	private static class SharedNodeManagerInvocationHandler implements InvocationHandler, Serializable {

		private final NodeManagerFactory targetFactory;

		SharedNodeManagerInvocationHandler(NodeManagerFactory targetFactory) {
			this.targetFactory = targetFactory;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			String methodName = method.getName();
			switch (methodName) {
				case "equals":
					// Only consider equal when proxies are identical.
					return (proxy == args[0]);
				case "hashCode":
					// Use hashCode of proxy.
					return hashCode();
				case "toString":
					// Deliver toString without touching a target Session.
					return "Shared Session proxy for target factory [" + this.targetFactory + "]";
				default:
					Function<NodeManager, Object> methodCall =
						targetSession -> ReflectionUtils.invokeMethod(method, targetSession, args);
					return invokeInTransaction(methodName, methodCall);
			}
		}

		private Object invokeInTransaction(String methodName, Function<NodeManager, Object> methodCall) {

			// Determine current Session: either the transactional one
			// managed by the factory or a temporary one for the given invocation.
			NodeManager target = Neo4jTransactionUtils.retrieveTransactionalNodeManager(this.targetFactory);

			if (TRANSACTION_REQUIRING_METHODS.contains(methodName)) {
				if (target == null || (!TransactionSynchronizationManager.isActualTransactionActive()
					&& target.getTransaction() != null)) {
					throw new IllegalStateException("No NodeManager with actual transaction available "
						+ "for current thread - cannot reliably process '" + methodName + "' call");
				}
			}

			// Regular Session operations.
			boolean isNewSession = false;
			if (target == null) {
				log.debug("Creating new Session for shared Session invocation");
				target = this.targetFactory.createNodeManager();
				isNewSession = true;
			}

			// Invoke method on current Session.
			try {
				return methodCall.apply(target);
			} finally {
				if (isNewSession) {
					target.flush();
				}
			}
		}
	}

	private SharedNodeManagerCreator() {
	}
}
