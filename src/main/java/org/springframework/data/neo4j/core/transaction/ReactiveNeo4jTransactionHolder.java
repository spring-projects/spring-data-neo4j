/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.core.transaction;

import reactor.core.publisher.Mono;

import java.util.Collection;

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactiveNeo4jTransactionHolder extends ResourceHolderSupport {

	private final Neo4jTransactionContext context;
	private final RxSession session;
	private final RxTransaction transaction;

	ReactiveNeo4jTransactionHolder(Neo4jTransactionContext context, RxSession session, RxTransaction transaction) {

		this.context = context;
		this.session = session;
		this.transaction = transaction;
	}

	RxSession getSession() {
		return session;
	}

	@Nullable
	RxTransaction getTransaction(DatabaseSelection inDatabase, UserSelection asUser) {

		return this.context.isForDatabaseAndUser(inDatabase, asUser) ? transaction : null;
	}

	Mono<Bookmark> commit() {

		return Mono.from(transaction.commit()).then(Mono.fromSupplier(session::lastBookmark));
	}

	Mono<Void> rollback() {

		return Mono.from(transaction.rollback());
	}

	Mono<Void> close() {

		return Mono.from(session.close());
	}

	DatabaseSelection getDatabaseSelection() {
		return context.getDatabaseSelection();
	}

	UserSelection getUserSelection() {
		return context.getUserSelection();
	}

	Collection<Bookmark> getBookmarks() {
		return context.getBookmarks();
	}
}
