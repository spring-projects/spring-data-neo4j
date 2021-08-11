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

import java.util.Optional;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Driver;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.mapping.context.AbstractMappingContext;

/**
 * Logs startup information.
 *
 * @author Michael J. Simons
 * @soundtrack Helge & Hardcore - Jazz
 */
final class StartupLogger {

	enum Mode {
		IMPERATIVE("imperative"), REACTIVE("reactive");

		final String displayValue;

		Mode(String displayValue) {
			this.displayValue = displayValue;
		}
	}

	private static final LogAccessor logger = new LogAccessor(LogFactory.getLog(StartupLogger.class));

	private final Mode mode;

	StartupLogger(Mode mode) {
		this.mode = mode;
	}

	void logStarting() {

		if (!logger.isDebugEnabled()) {
			return;
		}
		logger.debug(this::getStartingMessage);
	}

	String getStartingMessage() {

		StringBuilder sb = new StringBuilder();

		String sdnRx = getVersionOf(EnableNeo4jRepositories.class).map(v -> "SDN v" + v)
				.orElse("an unknown version of SDN");
		String sdC = getVersionOf(AbstractMappingContext.class).map(v -> "Spring Data Commons v" + v)
				.orElse("an unknown version of Spring Data Commons");
		String driver = getVersionOf(Driver.class).map(v -> "Neo4j Driver v" + v)
				.orElse("an unknown version of the Neo4j Java Driver");

		sb.append("Bootstrapping ").append(mode.displayValue).append(" Neo4j repositories based on ").append(sdnRx)
				.append(" with ").append(sdC).append(" and ").append(driver).append(".");

		return sb.toString();
	}

	private Optional<String> getVersionOf(Class<?> clazz) {

		return Optional.of(clazz).map(Class::getPackage).map(Package::getImplementationVersion).map(String::trim)
				.filter(v -> !v.isEmpty());
	}
}
