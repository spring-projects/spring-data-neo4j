/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.neo4j.springframework.data.integration.shared;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;

/**
 * Make sure that dynamic relationships can be loaded and stored.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Live At The Grugahalle
 */
@Neo4jIntegrationTest
public abstract class DynamicRelationshipsITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected final Driver driver;

	protected long idOfExistingPerson;

	protected DynamicRelationshipsITBase(Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	protected void setupData() {
		try (Transaction transaction = driver.session().beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			idOfExistingPerson = transaction.run(""
				+ "CREATE (t:PersonWithRelatives {name: 'A'}) WITH t "
				+ "CREATE (t) - [:HAS_WIFE] -> (w:Person {firstName: 'B'}) "
				+ "CREATE (t) - [:HAS_DAUGHTER] -> (d:Person {firstName: 'C'}) "
				+ " RETURN id(t) as id").single().get("id").asLong();
			transaction.commit();
		}
	}
}
