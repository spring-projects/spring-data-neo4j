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
package org.springframework.boot.autoconfigure.data.neo4j_rx;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Internal configuration of Neo4j client and transaction manager.
 *
 * @author Michael J. Simons
 */
@ConditionalOnClass({ Neo4jTransactionManager.class, PlatformTransactionManager.class })
@AutoConfigureAfter(Neo4jDriverAutoConfiguration.class)
@AutoConfigureBefore(Neo4jImperativeRepositoriesConfiguration.class)
class Neo4jImperativeDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Neo4jClient neo4jClient(Driver driver) {
		return Neo4jClient.create(driver);
	}

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public Neo4jTransactionManager transactionManager(Driver driver,
		ObjectProvider<TransactionManagerCustomizers> optionalCustomizers) {

		final Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(driver);
		optionalCustomizers.ifAvailable(customizer -> customizer.customize(transactionManager));

		return transactionManager;
	}
}
