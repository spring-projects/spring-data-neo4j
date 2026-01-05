/*
 * Copyright 2011-present the original author or authors.
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

import java.util.Collection;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.transaction.support.ResourceHolderSupport;

/**
 * Neo4j specific {@link ResourceHolderSupport resource holder}, wrapping a
 * {@link org.neo4j.driver.reactive.ReactiveTransaction}.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactiveNeo4jTransactionHolder extends ResourceHolderSupport {

	private final Neo4jTransactionContext context;

	private final ReactiveSession session;

	private final ReactiveTransaction transaction;

	ReactiveNeo4jTransactionHolder(Neo4jTransactionContext context, ReactiveSession session,
			ReactiveTransaction transaction) {

		this.context = context;
		this.session = session;
		this.transaction = transaction;
	}

	ReactiveSession getSession() {
		return this.session;
	}

	@Nullable ReactiveTransaction getTransaction(DatabaseSelection inDatabase, UserSelection asUser) {

		return this.context.isForDatabaseAndUser(inDatabase, asUser) ? this.transaction : null;
	}

	Mono<Set<Bookmark>> commit() {
		return Mono.fromDirect(this.transaction.commit()).then(Mono.fromSupplier(this.session::lastBookmarks));
	}

	Mono<Void> rollback() {
		return Mono.fromDirect(this.transaction.rollback()).then();
	}

	Mono<Void> close() {
		return Mono.fromDirect(this.session.close()).then();
	}

	DatabaseSelection getDatabaseSelection() {
		return this.context.getDatabaseSelection();
	}

	UserSelection getUserSelection() {
		return this.context.getUserSelection();
	}

	Collection<Bookmark> getBookmarks() {
		return this.context.getBookmarks();
	}

}
