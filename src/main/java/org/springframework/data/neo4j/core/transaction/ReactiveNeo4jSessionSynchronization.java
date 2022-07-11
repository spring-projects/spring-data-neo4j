/*
 * Copyright 2011-2022 the original author or authors.
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

import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;

import org.neo4j.driver.Driver;
import org.springframework.transaction.reactive.ReactiveResourceSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class ReactiveNeo4jSessionSynchronization
		extends ReactiveResourceSynchronization<ReactiveNeo4jTransactionHolder, Object> {

	private final ReactiveNeo4jTransactionHolder transactionHolder;

	ReactiveNeo4jSessionSynchronization(TransactionSynchronizationManager transactionSynchronizationManager,
			ReactiveNeo4jTransactionHolder transactionHolder, Driver driver) {

		super(transactionHolder, driver, transactionSynchronizationManager);

		this.transactionHolder = transactionHolder;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.reactive.ReactiveResourceSynchronization#shouldReleaseBeforeCompletion()
	 */
	@Override
	protected boolean shouldReleaseBeforeCompletion() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.reactive.ReactiveResourceSynchronization#processResourceAfterCommit(java.lang.Object)
	 */
	@Override
	protected Mono<Void> processResourceAfterCommit(ReactiveNeo4jTransactionHolder resourceHolder) {
		return Mono.defer(() -> super.processResourceAfterCommit(resourceHolder).then(resourceHolder.commit())).then();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.reactive.ReactiveResourceSynchronization#afterCompletion(int)
	 */
	@Override
	public Mono<Void> afterCompletion(int status) {
		return Mono.defer(() -> {
			if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				return transactionHolder.rollback().then(super.afterCompletion(status));
			}
			return super.afterCompletion(status);
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.transaction.reactive.ReactiveResourceSynchronization#releaseResource(java.lang.Object, java.lang.Object)
	 */
	@Override
	protected Mono<Void> releaseResource(ReactiveNeo4jTransactionHolder resourceHolder, Object resourceKey) {
		return Mono.defer(() -> JdkFlowAdapter.flowPublisherToFlux(resourceHolder.getSession().close()).then());
	}
}
