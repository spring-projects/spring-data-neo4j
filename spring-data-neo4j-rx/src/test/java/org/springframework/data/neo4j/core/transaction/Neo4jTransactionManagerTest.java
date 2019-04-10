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
package org.springframework.data.neo4j.core.transaction;

import static org.mockito.Mockito.*;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jTransactionManagerTest {

	@Mock
	private Driver driver;
	@Mock
	private Session session;
	@Mock
	private Transaction transaction;
	@Mock
	private StatementResult statementResult;

	@Test
	public void triggerCommitCorrectly() {

		when(driver.session(any(Consumer.class))).thenReturn(session);
		when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(transaction);
		when(transaction.run(anyString(), anyMap())).thenReturn(statementResult);
		when(session.isOpen()).thenReturn(true);
		when(transaction.isOpen()).thenReturn(true, false);

		Neo4jTransactionManager txManager = new Neo4jTransactionManager(driver);
		TransactionStatus txStatus = txManager.getTransaction(new DefaultTransactionDefinition());

		Neo4jClient client = Neo4jClient.create(driver);
		client.newQuery("RETURN 1").run();

		txManager.commit(txStatus);

		verify(driver).session(any(Consumer.class));

		verify(session).isOpen();
		verify(session).beginTransaction(any(TransactionConfig.class));

		verify(transaction, times(2)).isOpen();
		verify(transaction).success();
		verify(transaction).close();

		verify(session).close();
	}
}
