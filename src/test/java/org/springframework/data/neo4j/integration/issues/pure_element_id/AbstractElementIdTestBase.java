/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;

abstract class AbstractElementIdTestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	private static final Pattern NEO5J_ELEMENT_ID_PATTERN = Pattern.compile("\\d+:.+:\\d+");

	Predicate<String> validIdForCurrentNeo4j() {
		Predicate<String> nonNull = Objects::nonNull;

		if (neo4jConnectionSupport.isCypher5SyntaxCompatible()) {
			return nonNull.and(NEO5J_ELEMENT_ID_PATTERN.asMatchPredicate());
		}

		// Must be a valid long, and yes, I know how to write a pattern for that, too
		return nonNull.and(v -> {
			try {
				Long.parseLong(v);
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		});
	}

	static String adaptQueryTo44IfNecessary(String query) {
		if (!neo4jConnectionSupport.isCypher5SyntaxCompatible()) {
			query = query.replaceAll("elementId\\((.+?)\\)", "toString(id($1))");
		}
		return query;
	}

}
