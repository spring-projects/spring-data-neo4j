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
package org.springframework.data.neo4j.integration.issues.pure_element_id;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Tag(Neo4jExtension.NEEDS_VERSION_SUPPORTING_ELEMENT_ID)
abstract class AbstractElementIdTestBase {

	private static final Pattern NEO5J_ELEMENT_ID_PATTERN = Pattern.compile("\\d+:.+:\\d+");

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	static String adaptQueryTo44IfNecessary(String query) {
		if (!neo4jConnectionSupport.isCypher5SyntaxCompatible()) {
			query = query.replaceAll("elementId\\((.+?)\\)", "toString(id($1))");
		}
		return query;
	}

	static void assertThatLogMessageDoNotIndicateIDUsage(LogbackCapture logbackCapture) {
		List<String> formattedMessages = logbackCapture.getFormattedMessages();
		assertThat(formattedMessages)
			.noneMatch(s -> s.contains("Neo.ClientNotification.Statement.FeatureDeprecationWarning")
					|| s.contains("The query used a deprecated function. ('id' is no longer supported)")
					|| s.contains("The query used a deprecated function: `id`.")
					|| s.matches("(?s).*toString\\(id\\(.*")); // No deprecations are
																// logged when deprecated
																// function call is
																// nested. Anzeige ist
																// raus.
	}

	@BeforeEach
	void setupData(LogbackCapture logbackCapture, @Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture) {

		logbackCapture.addLogger("org.springframework.data.neo4j.cypher.deprecation", Level.WARN);
		logbackCapture.addLogger("org.springframework.data.neo4j.cypher", Level.DEBUG);
		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	Predicate<String> validIdForCurrentNeo4j() {
		Predicate<String> nonNull = Objects::nonNull;

		if (neo4jConnectionSupport.isCypher5SyntaxCompatible()) {
			return nonNull.and(NEO5J_ELEMENT_ID_PATTERN.asMatchPredicate());
		}

		// Must be a valid long, and yes, I know how to write a pattern for that, too
		return nonNull.and(v -> {
			try {
				Long.parseLong(v);
			}
			catch (NumberFormatException ex) {
				return false;
			}
			return true;
		});
	}

}
