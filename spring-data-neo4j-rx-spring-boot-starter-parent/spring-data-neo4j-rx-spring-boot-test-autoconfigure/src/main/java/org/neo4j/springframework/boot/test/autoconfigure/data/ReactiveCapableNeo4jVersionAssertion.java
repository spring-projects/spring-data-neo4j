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
package org.neo4j.springframework.boot.test.autoconfigure.data;

import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.util.ServerVersion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * This configuration is imported by {@link ReactiveDataNeo4jTest @ReactiveDataNeo4jTest} so that a sane exception
 * message is delivered when a user doesn't upgrade the test harness to 4.0.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@Configuration(proxyBeanMethods = false)
class ReactiveCapableNeo4jVersionAssertion implements InitializingBean {

	private final Driver driver;

	private static final String ERROR_MESSAGE = "@ReactiveDataNeo4jTest requires at least Neo4j version 4.0. "
		+ "The test support includes Neo4j test harness 3.5 by default. "
		+ "If you are on JDK 11+, include the following dependency in your project: "
		+ "org.neo4j.test:neo4j-harness:4.0.2.";

	ReactiveCapableNeo4jVersionAssertion(Driver driver) {
		this.driver = driver;
	}

	@Override
	public void afterPropertiesSet() {

		ServerVersion version = ServerVersion.version(driver);
		if (version.lessThan(ServerVersion.v4_0_0)) {
			throw new Neo4jVersionMismatchException(ERROR_MESSAGE, ServerVersion.v4_0_0.toString(), version.toString());
		}
	}
}
