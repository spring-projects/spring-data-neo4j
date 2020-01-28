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
package org.neo4j.springframework.data.examples.spring_boot;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.testcontainers.containers.Neo4jContainer;

/**
 * Start a test container and add the required properties to Springs environment to configure the driver
 *
 * @author Michael J. Simons
 */
final class TestContainerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
		final Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:4.0.0-enterprise")
			.withEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
		neo4jContainer.start();
		configurableApplicationContext
			.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> neo4jContainer.stop());
		TestPropertyValues
			.of(
				"org.neo4j.driver.uri=" + neo4jContainer.getBoltUrl(),
				"org.neo4j.driver.authentication.username=neo4j",
				"org.neo4j.driver.authentication.password=" + neo4jContainer.getAdminPassword()
			)
			.applyTo(configurableApplicationContext.getEnvironment());
	}
}
