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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;

/**
 * Make sure that dynamic relationships can be loaded and stored.
 *
 * @param <T> Type of the person with relatives
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Live At The Grugahalle
 */
@Neo4jIntegrationTest
public abstract class DynamicRelationshipsITBase<T> {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected final Driver driver;
	protected final BookmarkCapture bookmarkCapture;

	protected long idOfExistingPerson;

	protected final String labelOfTestSubject;

	protected DynamicRelationshipsITBase(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;

		Type type = getClass().getGenericSuperclass();
		String typeName = ((ParameterizedType) type).getActualTypeArguments()[0].getTypeName();
		this.labelOfTestSubject = typeName.substring(typeName.lastIndexOf(".") + 1);
	}

	@BeforeEach
	protected void setupData() {
		try (Session session = driver.session(); Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			idOfExistingPerson = transaction
					.run("" + "CREATE (t:" + labelOfTestSubject + " {name: 'A'}) WITH t "
							+ "CREATE (t) - [:HAS_WIFE] -> (w:Person {firstName: 'B'}) "
							+ "CREATE (t) - [:ACTIVE{performance:'average'}] -> (:Hobby {name: 'Biking'}) "
							+ "CREATE (t) - [:FOOTBALL{place:'Brunswick'}] -> (:Club {name: 'BTSV'}) "
							+ "CREATE (t) - [:HAS_DAUGHTER] -> (d:Person {firstName: 'C'}) " + "WITH t "
							+ "UNWIND ['Tom', 'Garfield'] AS cat " + "CREATE (t) - [:CATS] -> (w:Pet {name: cat}) "
							+ "WITH DISTINCT t " + "UNWIND ['Benji', 'Lassie'] AS dog "
							+ "CREATE (t) - [:DOGS] -> (w:Pet {name: dog}) " + "RETURN DISTINCT id(t) as id")
					.single().get("id").asLong();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}
}
