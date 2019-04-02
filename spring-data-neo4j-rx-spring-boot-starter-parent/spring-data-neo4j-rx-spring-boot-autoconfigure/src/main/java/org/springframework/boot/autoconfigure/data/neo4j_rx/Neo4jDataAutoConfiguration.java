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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Automatic configuration of base infrastructure for both standard and reactive Neo4j repositories. Depends on the
 * configured Neo4j driver.
 *
 * @author Michael J. Simons
 */
@Configuration
@ConditionalOnClass({ Driver.class, Neo4jTransactionManager.class, PlatformTransactionManager.class })
@ConditionalOnBean(Driver.class)
@AutoConfigureAfter(Neo4jDriverAutoConfiguration.class)
@AutoConfigureBefore({ Neo4jRepositoriesAutoConfiguration.class, Neo4jReactiveRepositoriesAutoConfiguration.class })
public class Neo4jDataAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Neo4jTemplate neo4jTemplate(Driver driver) {
		return new Neo4jTemplate(driver);
	}


	@Bean
	@ConditionalOnMissingBean
	public NodeManagerFactory nodeManagerFactory(Driver driver) {

		// TODO Scan classes like JPA or Mongo if not defined otherwise
		return new NodeManagerFactory(driver);
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
