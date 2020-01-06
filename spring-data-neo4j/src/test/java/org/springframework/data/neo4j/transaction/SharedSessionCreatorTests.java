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

import static org.assertj.core.api.Assertions.assertThat;
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
		assertThat(SharedSessionCreator.createSharedSession(sessionFactory)).isNotNull();
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
