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
package org.neo4j.springframework.boot.test.autoconfigure.data;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.springframework.boot.autoconfigure.Neo4jDriverAutoConfiguration;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Automatic configuration that provides {@link ServerControls} when the Neo4j test harness (community edition) is on the
 * classpath and a {@link Driver} using the internal Bolt connection to those controls.
 * <p>
 * By providing the {@link ServerControls} manually one can use the enterprise edition of the test harness as well, the
 * automatic configuration will then just configure the driver accordingly.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(ServerControls.class)
@ConditionalOnMissingBean(Driver.class)
@AutoConfigureBefore(Neo4jDriverAutoConfiguration.class)
public class Neo4jTestHarnessAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ServerControls.class)
	ServerControls neo4jServerControls() {
		return TestServerBuilders.newInProcessBuilder().newServer();
	}

	@Bean
	Driver neo4jDriver(ServerControls serverControls) {

		return GraphDatabase.driver(serverControls.boltURI());
	}
}
