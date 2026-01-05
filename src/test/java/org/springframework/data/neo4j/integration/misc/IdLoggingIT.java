/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.misc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.integration.bookmarks.DatabaseInitializer;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.ServerVersion;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@Neo4jIntegrationTest
@ExtendWith(LogbackCapturingExtension.class)
class IdLoggingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Configuration
	@EnableTransactionManagement
	@ComponentScan
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		DatabaseInitializer databaseInitializer(Driver driver) {
			return new DatabaseInitializer(driver);
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	static boolean isGreaterThanOrEqualNeo4j5() {
		return neo4jConnectionSupport.getServerVersion().greaterThanOrEqual(ServerVersion.v5_0_0);
	}

	@EnabledIf("isGreaterThanOrEqualNeo4j5")
	@Test
	void idWarningShouldBeSuppressed(LogbackCapture logbackCapture, @Autowired Neo4jClient neo4jClient) {

		// Was not able to combine the autowiring of capture and the client here
		for (Boolean enabled : new Boolean[] {true, false, null}) {

			Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger("org.springframework.data.neo4j.cypher.deprecation");
			Level originalLevel = logger.getLevel();
			logger.setLevel(Level.DEBUG);

			Boolean oldValue = null;
			if (enabled != null) {
				oldValue = Neo4jClient.SUPPRESS_ID_DEPRECATIONS.getAndSet(enabled);
			}

			try {
				assertThatCode(() -> neo4jClient.query(
						"CREATE (n:XXXIdTest) RETURN id(n)").fetch().all()).doesNotThrowAnyException();
				Predicate<String> stringPredicate = msg -> msg.contains(
						"Neo.ClientNotification.Statement.FeatureDeprecationWarning");

				if (enabled == null || enabled) {
					assertThat(logbackCapture.getFormattedMessages()).noneMatch(stringPredicate);
				} else {
					assertThat(logbackCapture.getFormattedMessages()).anyMatch(stringPredicate);
				}
			} finally {
				logbackCapture.clear();
				logger.setLevel(originalLevel);
				if (oldValue != null) {
					Neo4jClient.SUPPRESS_ID_DEPRECATIONS.set(oldValue);
				}
			}
		}
	}

	@EnabledIf("isGreaterThanOrEqualNeo4j5")
	@Test
	void otherDeprecationsWarningsShouldNotBeSuppressed(LogbackCapture logbackCapture, @Autowired Neo4jClient neo4jClient) {

		Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger("org.springframework.data.neo4j.cypher.deprecation");
		Level originalLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);

		try {
			assertThatCode(() -> neo4jClient.query(
					"MATCH (n) CALL {WITH n RETURN count(n) AS cnt} RETURN *").fetch().all()).doesNotThrowAnyException();
			assertThat(logbackCapture.getFormattedMessages())
					.anyMatch(msg -> msg.contains("Neo.ClientNotification.Statement.FeatureDeprecationWarning"))
					.anyMatch(msg -> msg.contains("CALL subquery without a variable scope clause is now deprecated. Use CALL (n) { ... }"));
		} finally {
			logger.setLevel(originalLevel);
		}
	}
}
