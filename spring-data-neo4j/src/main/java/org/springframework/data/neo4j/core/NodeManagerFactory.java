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

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.springframework.data.neo4j.core.schema.Schema;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.lang.Nullable;

/**
 * Creates ready to use instances of {@link NodeManager}.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.STABLE, since = "1.0")
@Slf4j
public final class NodeManagerFactory {

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	/**
	 * Driver that is used to create new sessions, either by directly invoking it or through Springs transactional utils.
	 */
	private final Driver driver;

	/**
	 * The initial set of classes that will be registered with the schema {@link #initialize()} to build the schema for node managers
	 * belonging to this factory.
	 */
	private final Set<Class<?>> initialPersistentClasses;

	@Nullable
	private Schema schema;

	/**
	 * Creates a new instance of a factory producing {@link NodeManager node managers}. When used in a transactional setup,
	 * i.e. with the {@link org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager}, make sure to use
	 * the same {@link Driver driver instance} for both the node and the transaction manager.
	 * <p>
	 * Spring Boots autoconfiguration for SDN RX will make sure that the same driver is used for both concerns.
	 *
	 * @param driver The driver used to obtain statement runners from when creating instances of node managers.
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

		if (!initialized.get()) {
			throw new IllegalStateException(
				"This factory has not been correctly initialized. Please provider a schema and register your persistent classes.");
		}
		// The call here to our Spring transaction shim has to be rethought in case we move this out of a Spring scope.
		// I dropped all the methods to configure that in an effort to make the setup more simple. ^mjs
		return new DefaultNodeManager(schema, Neo4jClient.create(driver),
			Neo4jTransactionUtils.retrieveTransaction(driver, null).orElse(null));
	}

	/**
	 * Provides the schema for this node manager factory. The schemas has to be set before a node manager is retrieved from this factory.
	 *
	 * @param schema
	 */
	public void setSchema(@Nullable Schema schema) {
		this.schema = schema;
	}

	/**
	 * This initializes this factory and is usually called by Springs infrastructure and only useful as standalone call
	 * when the node manager factory is used without Spring.
	 */
	public void initialize() {

		if (this.initialized.compareAndSet(false, true)) {
			Objects.requireNonNull(schema, "A schema is required. Did you provide one with #setSchema() beforehand?");
			log.info("Initializing schema with {} persistent classes", this.initialPersistentClasses.size());
			this.schema.register(this.initialPersistentClasses);
		}
	}
}
