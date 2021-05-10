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
package org.springframework.data.neo4j.integration.shared.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * Shared information for both imperative and reactive callbacks tests.
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public abstract class CallbacksITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;
	private final BookmarkCapture bookmarkCapture;

	private final Driver driver;

	protected CallbacksITBase(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	protected void setupData() {

		try (Transaction transaction = driver.session().beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.commit();
		}
	}

	protected void verifyDatabase(Iterable<ThingWithAssignedId> expectedValues) {

		List<String> ids = StreamSupport.stream(expectedValues.spliterator(), false).map(ThingWithAssignedId::getTheId)
				.collect(Collectors.toList());
		List<String> names = StreamSupport.stream(expectedValues.spliterator(), false).map(ThingWithAssignedId::getName)
				.collect(Collectors.toList());
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Record record = session
					.run("MATCH (n:Thing) WHERE n.theId in $ids RETURN COLLECT(n) as things", Values.parameters("ids", ids))
					.single();

			List<Node> nodes = record.get("things").asList(Value::asNode);
			assertThat(nodes).extracting(n -> n.get("theId").asString()).containsAll(ids);
			assertThat(nodes).extracting(n -> n.get("name").asString()).containsAll(names);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}
}
