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
package org.springframework.data.neo4j.core;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.StatementRunner;
import org.springframework.data.neo4j.core.transaction.ManagedTransactionProvider;
import org.springframework.data.neo4j.core.transaction.NativeTransactionProvider;

/**
 * Default implementation of {@link Neo4jOperations}. Uses the Neo4j Java driver to connect to and interact with the
 * database.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Neo4jTemplate implements Neo4jOperations {

	private final Driver driver;

	private final NativeTransactionProvider transactionProvider;

	public Neo4jTemplate(Driver driver) {

		this(driver, new ManagedTransactionProvider());
	}

	Neo4jTemplate(Driver driver, NativeTransactionProvider transactionProvider) {

		this.driver = driver;
		this.transactionProvider = transactionProvider;
	}

	AutoCloseableStatementRunner getStatementRunner() {

		StatementRunner statementRunner = transactionProvider.retrieveTransaction(driver)
			.map(StatementRunner.class::cast)
			.orElseGet(() -> driver.session());

		return (AutoCloseableStatementRunner) Proxy.newProxyInstance(StatementRunner.class.getClassLoader(),
			new Class<?>[] { AutoCloseableStatementRunner.class },
			new AutoCloseableStatementRunnerHandler(statementRunner));
	}

	@Override
	public Object executeQuery(String query) {

		try (AutoCloseableStatementRunner statementRunner = getStatementRunner()) {

			StatementResult result = statementRunner.run(query);

			return result.list();
		}
	}

	/**
	 * Makes a statement runner autoclosable and aware wether it's session or a transaction
	 */
	interface AutoCloseableStatementRunner extends StatementRunner, AutoCloseable {

		@Override void close();
	}

	private static class AutoCloseableStatementRunnerHandler implements InvocationHandler {

		private final Map<Method, MethodHandle> cachedHandles = new ConcurrentHashMap<>();
		private final StatementRunner target;

		AutoCloseableStatementRunnerHandler(StatementRunner target) {
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			if ("close".equals(method.getName())) {
				if (this.target instanceof Session) {
					((Session) this.target).close();
				}
				return null;
			} else {
				return cachedHandles.computeIfAbsent(method, this::findHandleFor)
					.invokeWithArguments(args);
			}
		}

		MethodHandle findHandleFor(Method method) {
			try {
				return MethodHandles.publicLookup().unreflect(method).bindTo(target);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}
	}
}

