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
package org.springframework.data.neo4j.repository.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 * @soundtrack Helge & Hardcore - Jazz
 */
class StartupLoggerTest {

	@Test
	void startingMessageShouldFit() {

		String message = new StartupLogger(StartupLogger.Mode.IMPERATIVE).getStartingMessage();
		assertThat(message).matches(
				"Bootstrapping imperative Neo4j repositories based on an unknown version of SDN with Spring Data Commons v2\\.\\d+\\.\\d+.(RELEASE|(?:(?:DATACMNS-)?\\d+-)?SNAPSHOT) and Neo4j Driver v4\\.\\d+\\.\\d+-.*\\.");
	}
}
