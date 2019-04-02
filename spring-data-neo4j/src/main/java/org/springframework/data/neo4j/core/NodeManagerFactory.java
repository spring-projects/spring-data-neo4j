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

import static org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils.*;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.StatementRunner;
import org.springframework.data.neo4j.core.schema.Scanner;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.lang.Nullable;

/**
 * Creates ready to use instances of {@link NodeManager}.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.STABLE, since = "1.0")
@Slf4j
public final class NodeManagerFactory  {

	/**
	 * Driver that is used to create new sessions, either by directly invoking it or through Springs transactional utils.
	 */
	private final Driver driver;

	/**
	 * The initial set of classes that will be scanned in {@link #initialize()} to build the schema for node managers
	 * belonging to this factory.
	 */
	private final Set<Class<?>> initialPersistentClasses;

	/** The scanner used to scan the initial set of persistent classes for creating a schema. */
	private Scanner scanner = new NoopScanner();

	@Nullable
	private Schema schema;

	private Function<Driver, StatementRunner> statementRunnerProvider = sourceDriver -> sourceDriver
		.session(defaultSessionParameters(null)).beginTransaction();

	/**
	 * Creates a new instance of a factory producing {@link NodeManager node managers}. When used in a transactional setup,
	 * i.e. with the {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}, make sure to use
	 * the same {@link Driver driver instance} for both the node and the transaction manager.
	 * <p>
	 * Spring Boots autoconfiguration for SDN RX will make sure that the same driver is used for both concerns.
	 *
	 * @param driver                   The driver used to obtain statement runners from when creating instances of node managers.
	 * @param initialPersistentClasses The set of classes that should be initially scanned
	 */
	public NodeManagerFactory(Driver driver, Class<?>... initialPersistentClasses) {
		this.driver = driver;

		this.initialPersistentClasses = new HashSet<>();
		Arrays.stream(initialPersistentClasses).forEach(this.initialPersistentClasses::add);
	}

	/**
	 * Creates a new node manager. The returned manager is supposed to have a short lifetime. When used in a Spring setup,
	 * this method should not called directly by client code. Instead the client code should use an injected instance of
	 * {@link NodeManager}, which will participate in Springs application based transaction or in JTA transactions.
	 *
	 * @return A new node manager
	 */
	public NodeManager createNodeManager() {

		Objects.requireNonNull(schema, "A schema is required. Did you call #initialize() before using this factory?");
		return new DefaultNodeManager(schema, statementRunnerProvider.apply(driver));
	}

	/**
	 * Configures a provider for extracting sessions/transactions from a Neo4j driver. This method is not to be called
	 * from application code and only used by internal API.
	 *
	 * @param statementRunnerProvider A required provider of statement runners
	 */
	@API(status = API.Status.INTERNAL, since = "1.0")
	public void setStatementRunnerProvider(Function<Driver, StatementRunner> statementRunnerProvider) {

		Objects.requireNonNull(statementRunnerProvider, "A node manager factory requires a provider of statement runners.");
		this.statementRunnerProvider = statementRunnerProvider;
	}

	/**
	 * Configures the scanner used to build a schema for domain objects. This method is not to be called from application
	 * code and only used by internal API.
	 *
	 * @param scanner
	 */
	@API(status = API.Status.INTERNAL, since = "1.0")
	public void setScanner(Scanner scanner) {

		Objects.requireNonNull(scanner, "A node manager factory requires a scanner.");
		this.scanner = scanner;
	}

	/**
	 * This initializes this factory and is usually called by Springs infrastructure and only useful as standalone call
	 * when the node manager factory is used without Spring.
	 */
	public void initialize() {

		log.info("Initializing schema with {} persistent classes", this.initialPersistentClasses.size());
		this.schema = scanner.scan(Collections.unmodifiableSet(this.initialPersistentClasses));
	}

	/**
	 * A noop implementation of a Schema scanner, only provided to create instances of a {@link NodeManagerFactory} that
	 * are in a valid state without booting up a whole context.
	 */
	private static class NoopScanner implements Scanner {

		@Override
		public Schema scan(Collection<Class<?>> persistentClasses) {
			return new Schema();
		}
	}
}
