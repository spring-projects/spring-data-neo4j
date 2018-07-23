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

import static org.hamcrest.core.Is.*;
import static org.hamcrest.core.IsNull.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/**
 * Unit tests for {@link SharedSessionCreator}.
 *
 * @author Mark Angrish
 */
public class SharedSessionCreatorTests {

	@Test
	public void proxyingWorksIfInfoReturnsNullSessionInterface() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		assertThat(SharedSessionCreator.createSharedSession(sessionFactory), is(notNullValue()));
	}

	@Test(expected = IllegalStateException.class)
	public void transactionRequiredExceptionOnPersist() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = SharedSessionCreator.createSharedSession(sessionFactory);
		session.save(new Object());
	}

	@Test(expected = IllegalStateException.class)
	public void transactionRequiredExceptionOnDelete() {
		SessionFactory sessionFactory = mock(SessionFactory.class);
		Session session = SharedSessionCreator.createSharedSession(sessionFactory);
		session.delete(new Object());
	}
}
