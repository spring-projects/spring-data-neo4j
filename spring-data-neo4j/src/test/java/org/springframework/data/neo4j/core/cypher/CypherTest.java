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
import static org.springframework.data.neo4j.core.cypher.Conditions.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;
import static org.springframework.data.neo4j.core.cypher.Functions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.cypher.renderer.Renderer;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
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
					.match(userNode.relationshipTo(bikeNode, "OWNS"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) RETURN b, u");
			}

			@Test
			void simpleRelationshipWithReturn() {
				Relationship owns = userNode
					.relationshipTo(bikeNode, "OWNS").named("o");

				Statement statement = Cypher
					.match(owns)
					.returning(bikeNode, userNode, owns)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN b, u, o");
			}

			@Test
			void chainedRelations() {
				Node tripNode = Cypher.node("Trip").named("t");
				Statement statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS").named("r1")
						.relationshipTo(tripNode, "USED_ON").named("r2")
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[r1:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(t:`Trip`) WHERE u.name =~ '.*aName' RETURN b, u");

				statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS")
						.relationshipTo(tripNode, "USED_ON").named("r2")
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(t:`Trip`) WHERE u.name =~ '.*aName' RETURN b, u");


				statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS")
						.relationshipTo(tripNode, "USED_ON").named("r2")
						.relationshipFrom(userNode, "WAS_ON").named("x")
						.relationshipBetween(Cypher.node("SOMETHING")).named("y")
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`]->(t:`Trip`)<-[x:`WAS_ON`]-(u)-[y]-(:`SOMETHING`) WHERE u.name =~ '.*aName' RETURN b, u");
			}

			@Test
			void sortOrderDefault() {
				Statement statement = Cypher.match(userNode).returning(userNode).orderBy(sort(userNode.property("name"))).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name");
			}

			@Test
			void sortOrderAscending() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(Cypher.sort(userNode.property("name")).ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderDescending() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(Cypher.sort(userNode.property("name")).descending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC");
			}

			@Test
			void sortOrderConcatenation() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(
						sort(userNode.property("name")).descending(),
						sort(userNode.property("age")).ascending()
					)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC, u.age ASC");
			}

			@Test
			void sortOrderDefaultExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name").ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderAscendingExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name").ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderDescendingExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name").descending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC");
			}

			@Test
			void sortOrderConcatenationExpression() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(userNode.property("name")).descending()
					     .and(userNode.property("age")).ascending()
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name DESC, u.age ASC");
			}

			@Test
			void skip() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u SKIP 1");
			}

			@Test
			void limit() {
				Statement statement = Cypher.match(userNode).returning(userNode).limit(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u LIMIT 1");
			}

			@Test
			void skipAndLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(1).limit(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u SKIP 1 LIMIT 1");
			}
		}
	}

	@Nested
	class MultipleMatches {
		@Test
		void simple() {
			Statement statement = Cypher
				.match(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) RETURN b");
		}

		@Test
		void simpleWhere() {
			Statement statement = Cypher
				.match(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void multiWhere() {
			Statement statement = Cypher
				.match(bikeNode)
				.where(bikeNode.property("a").isNotNull())
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) WHERE b.a IS NOT NULL MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void multiWhereMultiConditions() {
			Statement statement = Cypher
				.match(bikeNode)
				.where(bikeNode.property("a").isNotNull())
				.and(bikeNode.property("b").isNull())
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull().or(userNode.internalId().isEqualTo(literalOf(4711))))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) WHERE (b.a IS NOT NULL AND b.b IS NULL) MATCH (u:`User`), (o:`U`) WHERE (u.a IS NULL OR id(u) = 4711) RETURN b");
		}

		@Test
		void optional() {
			Statement statement = Cypher
				.optional()
				.match(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("OPTIONAL MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void optionalNext() {
			Statement statement = Cypher
				.match(bikeNode)
				.optional()
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) OPTIONAL MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}
	}

	@Nested
	class FunctionRendering {
		@Test
		void inWhereClause() {
			Statement statement = Cypher.match(userNode).where(userNode.internalId().isEqualTo(literalOf(1L)))
				.returning(userNode).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE id(u) = 1 RETURN u");
		}

		@Test
		void inReturnClause() {
			Statement statement = Cypher.match(userNode).returning(Functions.count(userNode)).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN count(u)");
		}

		@Test
		void aliasedInReturnClause() {
			Statement statement = Cypher.match(userNode).returning(Functions.count(userNode).as("cnt")).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN count(u) AS cnt");
		}

		@Test
		void shouldSupportMoreThanOneArgument() {
			Statement statement = Cypher.match(userNode)
				.returning(coalesce(userNode.property("a"), userNode.property("b"), Cypher.literalOf("¯\\_(ツ)_/¯")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN coalesce(u.a, u.b, '¯\\\\_(ツ)_/¯')");
		}
	}

	@Nested
	class ComparisonRendering {

		@Test
		void equalsWithStringLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(Cypher.literalOf("Test")))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.name = 'Test' RETURN u");
		}

		@Test
		void equalsWithNumberLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("age").isEqualTo(Cypher.literalOf(21)))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.age = 21 RETURN u");
		}

		@Test
		void equalsWithObjectLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("field").isEqualTo(Cypher.literalOf(new CustomType())))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.field = Value RETURN u");
		}


		private class CustomType {
			@Override
			public String toString() {
				return "Value";
			}
		}
	}

	@Nested
	class Conditions {
		@Test
		void conditionsChainingAnd() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(Cypher.literalOf("Test"))
						.and(userNode.property("age").isEqualTo(Cypher.literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' AND u.age = 21) RETURN u");
		}

		@Test
		void conditionsChainingOr() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(Cypher.literalOf("Test"))
						.or(userNode.property("age").isEqualTo(Cypher.literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' OR u.age = 21) RETURN u");
		}

		@Test
		void conditionsChainingXor() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(Cypher.literalOf("Test"))
						.xor(userNode.property("age").isEqualTo(Cypher.literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' XOR u.age = 21) RETURN u");
		}

		@Test
		void chainingOnWhere() {
			Statement statement;

			Literal test = literalOf("Test");
			Literal foobar = literalOf("foobar");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE u.name = 'Test' RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' AND u.name = 'Test' AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' OR u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE (u.name = 'Test' OR u.name = 'Test' OR u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((u.name = 'Test' AND u.name = 'Test') OR u.name = 'foobar') AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name = 'Test' OR u.name = 'foobar') AND u.name = 'Test' AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.or(userNode.property("name").isEqualTo(foobar))
				.and(userNode.property("name").isEqualTo(test))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((((u.name = 'Test' OR u.name = 'foobar') AND u.name = 'Test') OR u.name = 'foobar') AND u.name = 'Test') RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isNotNull())
				.and(userNode.property("name").isEqualTo(test))
				.or(userNode.property("age").isEqualTo(Cypher.literalOf(21)))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name IS NOT NULL AND u.name = 'Test') OR u.age = 21) RETURN u");
		}

		@Test
		void chainingOnConditions() {
			Statement statement;

			Literal test = literalOf("Test");
			Literal foobar = literalOf("foobar");
			Literal bazbar = literalOf("bazbar");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' OR u.name = 'foobar' OR u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test))
				.and(
					userNode.property("name").isEqualTo(bazbar)
						.and(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' AND u.name = 'bazbar' AND u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE ((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') RETURN u");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
						.and(userNode.property("name").isEqualTo(bazbar))
				)
				.returning(userNode)
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') AND u.name = 'bazbar') RETURN u");
		}

		@Test
		void chainingCombined() {
			Statement statement;

			Literal test = literalOf("Test");
			Literal foobar = literalOf("foobar");
			Literal bazbar = literalOf("bazbar");

			statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(test)
						.and(userNode.property("name").isEqualTo(bazbar))
						.or(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(foobar))
				)
				.and(
					userNode.property("name").isEqualTo(bazbar)
						.and(userNode.property("name").isEqualTo(foobar))
						.or(userNode.property("name").isEqualTo(test))
						.not()
				)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((u.name = 'Test' AND u.name = 'bazbar') OR u.name = 'foobar' OR u.name = 'foobar') AND NOT (((u.name = 'bazbar' AND u.name = 'foobar') OR u.name = 'Test'))) RETURN u");

		}

		@Test
		void negatedConditions() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("name").isNotNull().not())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE NOT (u.name IS NOT NULL) RETURN u");
		}

		@Test
		void noConditionShouldNotBeRendered() {
			Statement statement;
			statement = Cypher.match(userNode)
				.where(noCondition())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(Cypher.literalOf("test")))
				.and(noCondition()).or(noCondition())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.name = 'test' RETURN u");
		}
	}

	@Nested
	class DeleteClause {

		@Test
		void shouldRenderDeleteWithoutReturn() {

			Statement statement;
			statement = Cypher.match(userNode)
				.detach().delete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) DETACH DELETE u");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.delete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DELETE u");

			statement = Cypher.match(userNode, bikeNode)
				.delete(userNode, bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`), (b:`Bike`) DELETE u, b");
		}

		@Test
		void shouldRenderDeleteWithReturn() {

			Statement statement;
			statement = Cypher.match(userNode)
				.detach().delete(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) DETACH DELETE u RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.detach().delete(userNode)
				.returning(userNode).orderBy(userNode.property("a").ascending()).skip(2).limit(1)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DETACH DELETE u RETURN u ORDER BY u.a ASC SKIP 2 LIMIT 1");

		}

		@Test
		void shouldRenderNodeDelete() {
			Node n = anyNode("n");
			Relationship r = n.relationshipBetween(anyNode()).named("r0");
			Statement statement = Cypher
				.match(n).where(n.internalId().isEqualTo(literalOf(4711)))
				.optional().match(r)
				.delete(r, n)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) WHERE id(n) = 4711 OPTIONAL MATCH (n)-[r0]-() DELETE r0, n");
		}
	}

	@Nested
	class Expressions {
		@Test
		void shouldRenderParameters() {
			Statement statement;
			statement = Cypher.match(userNode)
				.where(userNode.property("a").isEqualTo(parameter("aParameter")))
				.detach().delete(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.a = $aParameter DETACH DELETE u RETURN u");
		}
	}
}
