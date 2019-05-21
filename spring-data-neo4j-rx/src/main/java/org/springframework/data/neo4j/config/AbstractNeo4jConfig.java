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
package org.springframework.data.neo4j.config;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jClient;

/**
 * Base class for imperative SDN-RX configuration using JavaConfig.
 * This can be included in all scenarios in which Spring Boot is not an option.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
@Configuration
@API(status = API.Status.STABLE, since = "1.0")
public abstract class AbstractNeo4jConfig extends Neo4jConfigurationSupport {

	/**
	 * The driver used here should be the driver resulting from {@link #driver()}, which is the default.
	 *
	 * @param driver The driver to connect with.
	 * @return A imperative Neo4j client.
	 */
	@Bean
	public Neo4jClient neo4jClient(Driver driver) {
		return Neo4jClient.create(driver);
	}
}
