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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.data.neo4j.core.Neo4jTemplate.AutoCloseableStatementRunner;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jTemplateTest {

	@Mock
	private Driver driver;
	@Mock
	private Session session;
	@Mock
	private Transaction transaction;

	@Nested
	class AutoCloseableStatementRunnerHandlerTest {

		@Test
		public void shouldCallCloseOnSession() {

			when(driver.session()).thenReturn(session);

			// Make template acquire session
			Neo4jTemplate neo4jTemplate = new Neo4jTemplate(driver, (d, n) -> Optional.empty());
			try (AutoCloseableStatementRunner s = neo4jTemplate.getStatementRunner()) {
				s.run("MATCH (n) RETURN n");
			}

			verify(driver).session();
			verify(session).run(any(String.class));
			verify(session).close();
			verifyNoMoreInteractions(driver, session, transaction);
		}

		@Test
		public void shouldNotInvokeCloseOnTransaction() {

			Neo4jTemplate neo4jTemplate = new Neo4jTemplate(driver, (d, n) -> Optional.of(transaction));
			try (AutoCloseableStatementRunner s = neo4jTemplate.getStatementRunner()) {
				s.run("MATCH (n) RETURN n");
			}

			verify(transaction).run(any(String.class));
			verifyNoMoreInteractions(driver, session, transaction);
		}
	}
}
