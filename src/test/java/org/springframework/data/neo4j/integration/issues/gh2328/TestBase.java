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
package org.springframework.data.neo4j.integration.issues.gh2328;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 * @soundtrack Motörhead - Better Motörhead Than Dead - Live At Hammersmith
 */
abstract class TestBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static UUID id;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH (n) detach delete n");
			id = UUID.fromString(
					transaction.run("CREATE (f:SomeEntity {name: 'A name', id: randomUUID()}) RETURN f.id").single()
							.get(0).asString());
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	protected final boolean requirements(SomeEntity someEntity) {
		assertThat(someEntity).isNotNull();
		assertThat(someEntity).extracting(SomeEntity::getId).isEqualTo(id);
		assertThat(someEntity).extracting(SomeEntity::getName).isEqualTo("A name");
		return true;
	}
}
