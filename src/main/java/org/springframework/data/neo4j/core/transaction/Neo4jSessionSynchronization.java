/*
 * Copyright 2011-2025 the original author or authors.
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

import org.neo4j.driver.Driver;

import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Neo4j specific {@link ResourceHolderSynchronization} for resource cleanup at the end of
 * a transaction when participating in a non-native Neo4j transaction, such as a Jta
 * transaction.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 6.0
 */
final class Neo4jSessionSynchronization extends ResourceHolderSynchronization<Neo4jTransactionHolder, Object> {

	private final Neo4jTransactionHolder localConnectionHolder;

	Neo4jSessionSynchronization(Neo4jTransactionHolder connectionHolder, Driver driver) {

		super(connectionHolder, driver);
		this.localConnectionHolder = connectionHolder;
	}

	@Override
	protected boolean shouldReleaseBeforeCompletion() {
		return false;
	}

	@Override
	protected void processResourceAfterCommit(Neo4jTransactionHolder resourceHolder) {

		super.processResourceAfterCommit(resourceHolder);

		if (resourceHolder.hasActiveTransaction()) {
			resourceHolder.commit();
		}
	}

	@Override
	public void afterCompletion(int status) {

		if (status == TransactionSynchronization.STATUS_ROLLED_BACK
				&& this.localConnectionHolder.hasActiveTransaction()) {
			this.localConnectionHolder.rollback();
		}

		super.afterCompletion(status);
	}

	@Override
	protected void releaseResource(Neo4jTransactionHolder resourceHolder, Object resourceKey) {

		if (resourceHolder.hasActiveSession()) {
			resourceHolder.close();
		}
	}

}
