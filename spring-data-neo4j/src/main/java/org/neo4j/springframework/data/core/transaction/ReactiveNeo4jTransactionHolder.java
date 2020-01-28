/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.transaction;

import static org.neo4j.springframework.data.core.transaction.Neo4jTransactionUtils.*;

import reactor.core.publisher.Mono;

import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
class ReactiveNeo4jTransactionHolder extends ResourceHolderSupport {

	private final RxSession session;
	private final String databaseName;
	private final RxTransaction transaction;

	ReactiveNeo4jTransactionHolder(String databaseName, RxSession session, RxTransaction transaction) {

		this.session = session;
		this.databaseName = databaseName;
		this.transaction = transaction;
	}

	RxSession getSession() {
		return session;
	}

	@Nullable RxTransaction getTransaction(String inDatabase) {

		return namesMapToTheSameDatabase(this.databaseName, inDatabase) ? transaction : null;
	}

	Mono<Void> commit() {

		return Mono.from(transaction.commit());
	}

	Mono<Void> rollback() {

		return Mono.from(transaction.rollback());
	}

	Mono<Void> close() {

		return Mono.from(session.close());
	}

	@Override
	public void setRollbackOnly() {

		super.setRollbackOnly();
	}

	@Override
	public void resetRollbackOnly() {

		throw new UnsupportedOperationException();
	}

	String getDatabaseName() {
		return databaseName;
	}
}
