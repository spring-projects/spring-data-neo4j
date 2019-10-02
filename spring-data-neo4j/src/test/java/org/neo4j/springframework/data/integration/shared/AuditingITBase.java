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

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;

/**
 * Shared information for both imperative and reactive auditing tests.
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public abstract class AuditingITBase {
	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	protected static final String EXISTING_THING_NAME = "An old name";
	protected static final String EXISTING_THING_CREATED_BY = "The creator";
	protected static final LocalDateTime EXISTING_THING_CREATED_AT = LocalDateTime.of(2013, 5, 6, 8, 0);
	protected static final LocalDateTime DEFAULT_CREATION_AND_MODIFICATION_DATE = LocalDateTime.of(2018, 7, 1, 8, 0);

	private final Driver driver;

	protected Long idOfExistingThing;

	protected AuditingITBase(Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	protected void setupData() {
		try (Transaction transaction = driver.session().beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			idOfExistingThing = transaction
				.run(
					"CREATE (t:ImmutableAuditableThing {name: $name, createdBy: $createdBy, createdAt: $createdAt}) RETURN id(t) as id",
					Values.parameters("name", EXISTING_THING_NAME, "createdBy", EXISTING_THING_CREATED_BY, "createdAt",
						EXISTING_THING_CREATED_AT))
				.single().get("id").asLong();

			transaction.commit();
		}
	}

	protected void verifyDatabase(long id, ImmutableAuditableThing expectedValues) {

		try (Session session = driver.session()) {
			Node node = session
				.run("MATCH (t:ImmutableAuditableThing) WHERE id(t) = $id RETURN t", Values.parameters("id", id))
				.single().get("t").asNode();

			assertThat(node.get("name").asString()).isEqualTo(expectedValues.getName());
			assertThat(node.get("createdAt").asLocalDateTime()).isEqualTo(expectedValues.getCreatedAt());
			assertThat(node.get("createdBy").asString()).isEqualTo(expectedValues.getCreatedBy());

			Value modifiedAt = node.get("modifiedAt");
			Value modifiedBy = node.get("modifiedBy");

			if (expectedValues.getModifiedAt() == null) {
				assertThat(modifiedAt.isNull()).isTrue();
			} else {
				assertThat(modifiedAt.asLocalDateTime()).isEqualTo(expectedValues.getModifiedAt());
			}

			if (expectedValues.getModifiedBy() == null) {
				assertThat(modifiedBy.isNull()).isTrue();
			} else {
				assertThat(modifiedBy.asString()).isEqualTo(expectedValues.getModifiedBy());
			}
		}
	}
}
