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

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public abstract class IdGeneratorsITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static final String EXISTING_THING_NAME = "An old name";

	protected static final String ID_OF_EXISTING_THING = "not-generated.";

	private final Driver driver;

	private final BookmarkCapture bookmarkCapture;

	protected IdGeneratorsITBase(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	protected void setupData() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (t:ThingWithGeneratedId {name: $name, theId: $theId}) RETURN id(t) as id",
					Values.parameters("name", EXISTING_THING_NAME, "theId", ID_OF_EXISTING_THING));
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	protected void verifyDatabase(String id, String name) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Node node = session.run("MATCH (t) WHERE t.theId = $theId RETURN t", Values.parameters("theId", id)).single()
					.get("t").asNode();

			assertThat(node.get("name").asString()).isEqualTo(name);
			assertThat(node.get("theId").asString()).isEqualTo(id);
		}
	}
}
