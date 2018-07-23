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

import org.neo4j.ogm.session.Session;
import org.springframework.transaction.support.ResourceHolderSupport;
import org.springframework.util.Assert;

/**
 * Holder wrapping a Neo4j OGM Session. Neo4jTransactionManager binds instances of this class to the thread, for a given
 * SessionFactory.
 * <p>
 * Note: This is an SPI class, not intended to be used by applications.
 *
 * @author Mark Angrish
 * @see Neo4jTransactionManager
 * @see SessionFactoryUtils
 */
public class SessionHolder extends ResourceHolderSupport {

	private final Session session;

	private boolean transactionActive;

	public SessionHolder(Session session) {
		Assert.notNull(session, "Session must not be null");
		this.session = session;
	}

	public Session getSession() {
		return this.session;
	}

	protected void setTransactionActive(boolean transactionActive) {
		this.transactionActive = transactionActive;
	}

	protected boolean isTransactionActive() {
		return this.transactionActive;
	}

	@Override
	public void clear() {
		super.clear();
		this.transactionActive = false;
	}
}
