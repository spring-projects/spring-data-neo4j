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
package org.springframework.data.neo4j.core.cypher;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;

/**
 * @author Michael J. Simons
 */
public class CypherTest {

	private final Renderer cypherRenderer = CypherRenderer.create();
	private final Node bikeNode = Cypher.node("Bike").named("b");
	private final Node userNode = Cypher.node("User").named("u");

	@Nested
	class SingleQuerySinglePart {

		@Nested
		class ReadingAndReturn {

			@Test
			void unrelatedNodes() {
				Statement statement = Cypher.match(bikeNode, userNode, Cypher.node("U").named("o"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (b:`Bike`), (u:`User`), (o:`U`) RETURN b, u");
			}

			@Test
			void aliasedExpressionsInReturn() {
				Node unnamedNode = Cypher.node("ANode");
				Node namedNode = Cypher.node("AnotherNode").named("o");
				Statement statement = Cypher.match(unnamedNode, namedNode)
					.returning(unnamedNode.as("theNode"), namedNode.as("theOtherNode"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (:`ANode`), (o:`AnotherNode`) RETURN (:`ANode`) AS theNode, o AS theOtherNode");
			}

			@Test
			void simpleRelationship() {
				Statement statement = Cypher
					.match(userNode.outgoingRelationShipTo(bikeNode).withType("OWNS").create())
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) RETURN b, u");
			}

			@Test
			void simpleRelationshipWithReturn() {
				Relationship owns = userNode
					.outgoingRelationShipTo(bikeNode).withType("OWNS").named("o").create();

				Statement statement = Cypher
					.match(owns)
					.returning(bikeNode, userNode, owns)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN b, u, o");
			}

			@Test
			void chainedRelations() {
				Node tripNode = Cypher.node("Trip").named("u");
				Statement statement = Cypher
					.match(userNode
						.outgoingRelationShipTo(bikeNode).withType("OWNS").named("r1")
						.outgoingRelationShipTo(tripNode).withType("USED_ON").named("r2")
						.create()
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[r1:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(u:`Trip`) WHERE u.name =~ '.*aName' RETURN b, u");
			}
		}

	}
}
