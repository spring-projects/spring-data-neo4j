/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.cypher;

import static org.assertj.core.api.Assertions.*;
import static org.neo4j.springframework.data.core.cypher.Conditions.exists;
import static org.neo4j.springframework.data.core.cypher.Conditions.not;
import static org.neo4j.springframework.data.core.cypher.Conditions.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.cypher.Functions.*;

import java.util.function.Function;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
class CypherIT {

	private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();
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
			void asteriskShouldWork() {
				Statement statement = Cypher.match(bikeNode, userNode, Cypher.node("U").named("o"))
					.returning(asterisk())
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (b:`Bike`), (u:`User`), (o:`U`) RETURN *");
			}

			@Test
			void aliasedExpressionsInReturn() {
				Node unnamedNode = Cypher.node("ANode");
				Node namedNode = Cypher.node("AnotherNode").named("o");
				Statement statement = Cypher.match(unnamedNode, namedNode)
					.returning(namedNode.as("theOtherNode"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (:`ANode`), (o:`AnotherNode`) RETURN o AS theOtherNode");
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

			@Test // GH-169
			void multipleRelationshipTypes() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS", "RIDES"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`|`RIDES`]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-170
			void relationshipWithProperties() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").properties(mapOf("boughtOn", literalOf("2019-04-16"))))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS` {boughtOn: '2019-04-16'}]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithMinimumLength() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").min(3))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*3..]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithMaximumLength() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").max(5))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*..5]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithLength() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").length(3, 5))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*3..5]->(b:`Bike`) RETURN b, u");
			}

			@Test // GH-168
			void relationshipWithLengthAndProperties() {
				Statement statement = Cypher
					.match(userNode.relationshipTo(bikeNode, "OWNS").length(3, 5).properties(mapOf("boughtOn", literalOf("2019-04-16"))))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (u:`User`)-[:`OWNS`*3..5 {boughtOn: '2019-04-16'}]->(b:`Bike`) RETURN b, u");
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

			@Test // GH-177
			void chainedRelationshipsWithPropertiesAndLength() {
				Node tripNode = Cypher.node("Trip").named("t");
				Statement statement = Cypher
					.match(userNode
						.relationshipTo(bikeNode, "OWNS")
						.relationshipTo(tripNode, "USED_ON").named("r2").min(1).properties(mapOf("when", literalOf("2019-04-16")))
						.relationshipFrom(userNode, "WAS_ON").named("x").max(2).properties("whatever", literalOf("2020-04-16"))
						.relationshipBetween(Cypher.node("SOMETHING")).named("y").length(2, 3).properties(mapOf("idk", literalOf("2021-04-16")))
					)
					.where(userNode.property("name").matches(".*aName"))
					.returning(bikeNode, userNode)
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`)-[r2:`USED_ON`*1.. {when: '2019-04-16'}]->(t:`Trip`)<-[x:`WAS_ON`*..2 {whatever: '2020-04-16'}]-(u)-[y*2..3 {idk: '2021-04-16'}]-(:`SOMETHING`) WHERE u.name =~ '.*aName' RETURN b, u");
			}

			@Test // GH-182
			void sizeOfRelationship() {

				Statement statement = Cypher
					.match(anyNode("a"))
					.where(property("a", "name").isEqualTo(literalOf("Alice")))
					.returning(Functions.size(anyNode("a").relationshipTo(anyNode())).as("fof"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (a) WHERE a.name = 'Alice' RETURN size((a)-->()) AS fof");
			}

			@Test // GH-182
			void sizeOfRelationshipChain() {

				Statement statement = Cypher
					.match(anyNode("a"))
					.where(property("a", "name").isEqualTo(literalOf("Alice")))
					.returning(Functions.size(anyNode("a").relationshipTo(anyNode()).relationshipTo(anyNode())).as("fof"))
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo("MATCH (a) WHERE a.name = 'Alice' RETURN size((a)-->()-->()) AS fof");
			}

			@Test
			void sortOrderDefault() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(sort(userNode.property("name"))).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name");
			}

			@Test
			void sortOrderAscending() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(sort(userNode.property("name")).ascending()).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u ORDER BY u.name ASC");
			}

			@Test
			void sortOrderDescending() {
				Statement statement = Cypher.match(userNode).returning(userNode)
					.orderBy(sort(userNode.property("name")).descending()).build();

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
			void nullSkip() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(null).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u");
			}

			@Test
			void limit() {
				Statement statement = Cypher.match(userNode).returning(userNode).limit(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u LIMIT 1");
			}

			@Test
			void nullLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).limit(null).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u");
			}

			@Test
			void skipAndLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(1).limit(1).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u SKIP 1 LIMIT 1");
			}

			@Test
			void nullSkipAndLimit() {
				Statement statement = Cypher.match(userNode).returning(userNode).skip(null).limit(null).build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN u");
			}

			@Test
			void distinct() {
				Statement statement = Cypher.match(userNode).returningDistinct(userNode).skip(1).limit(1).build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (u:`User`) RETURN DISTINCT u SKIP 1 LIMIT 1");
			}
		}
	}

	@Nested
	class SingleQueryMultiPart {
		@Test
		void simpleWith() {
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.with(bikeNode, userNode)
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL WITH b, u RETURN b");
		}

		@Test
		void shouldRenderLeadingWith() {
			Statement statement = Cypher
				.with(Cypher.parameter("listOfPropertyMaps").as("p"))
				.unwind("p").as("item")
				.returning("item")
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("WITH $listOfPropertyMaps AS p UNWIND p AS item RETURN item");
		}

		@Test
		void simpleWithChained() {

			Node tripNode = Cypher.node("Trip").named("t");
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.with(bikeNode, userNode)
				.match(tripNode)
				.where(tripNode.property("name").isEqualTo(literalOf("Festive500")))
				.with(tripNode)
				.returning(bikeNode, userNode, tripNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL WITH b, u MATCH (t:`Trip`) WHERE t.name = 'Festive500' WITH t RETURN b, u, t");
		}

		@Test
		void deletingSimpleWith() {
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.delete(userNode)
				.with(bikeNode, userNode)
				.returning(bikeNode, userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL DELETE u WITH b, u RETURN b, u");
		}

		@Test
		void deletingSimpleWithReverse() {
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.where(userNode.property("a").isNull())
				.with(bikeNode, userNode)
				.delete(userNode)
				.returning(bikeNode, userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) WHERE u.a IS NULL WITH b, u DELETE u RETURN b, u");
		}

		@Test
		void mixedClausesWithWith() {

			Node tripNode = Cypher.node("Trip").named("t");
			Statement statement = Cypher
				.match(userNode.relationshipTo(bikeNode, "OWNS"))
				.match(tripNode)
				.delete(tripNode)
				.with(bikeNode, tripNode)
				.match(userNode)
				.with(bikeNode, userNode)
				.returning(bikeNode, userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`)-[:`OWNS`]->(b:`Bike`) MATCH (t:`Trip`) DELETE t WITH b, t MATCH (u) WITH b, u RETURN b, u");
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
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a IS NOT NULL MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
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
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE (b.a IS NOT NULL AND b.b IS NULL) MATCH (u:`User`), (o:`U`) WHERE (u.a IS NULL OR id(u) = 4711) RETURN b");
		}

		@Test
		void optional() {
			Statement statement = Cypher
				.optionalMatch(bikeNode)
				.match(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("OPTIONAL MATCH (b:`Bike`) MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void usingSameWithStepWithoutReassign() {
			StatementBuilder.OrderableOngoingReadingAndWith firstStep = match(bikeNode).with(bikeNode);

			firstStep.optionalMatch(userNode);
			firstStep.optionalMatch(Cypher.node("Trip"));

			Statement statement = firstStep.returning(Cypher.asterisk()).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) WITH b OPTIONAL MATCH (u:`User`) OPTIONAL MATCH (:`Trip`) RETURN *");
		}

		@Test
		void usingSameWithStepWithoutReassignThenUpdate() {
			StatementBuilder.OrderableOngoingReadingAndWith firstStep = match(bikeNode).with(bikeNode);

			firstStep.optionalMatch(userNode);
			firstStep.optionalMatch(Cypher.node("Trip"));
			firstStep.delete("u");

			Statement statement = firstStep.returning(Cypher.asterisk()).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WITH b OPTIONAL MATCH (u:`User`) OPTIONAL MATCH (:`Trip`) DELETE u RETURN *");
		}

		@Test
		void usingSameWithStepWithReassign() {
			ExposesMatch firstStep = match(bikeNode).with(bikeNode);

			firstStep = firstStep.optionalMatch(userNode);
			firstStep = firstStep.optionalMatch(Cypher.node("Trip"));

			Statement statement = ((ExposesReturning) firstStep).returning(Cypher.asterisk()).build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) WITH b OPTIONAL MATCH (u:`User`) OPTIONAL MATCH (:`Trip`) RETURN *");
		}

		@Test
		void queryPartsShouldBeExtractableInQueries() {

			// THose can be a couple of queries ending in a WITH statement so the
			// pipeline they present in the full query is also present in Java.
			Function<ExposesMatch, ExposesMatch> step1Supplier =
				previous -> previous.match(node("S1").named("n")).where(property("n", "a").isEqualTo(literalOf("A"))).with("n");
			Function<ExposesMatch, ExposesReturning> step2Supplier =
				previous -> previous.match(Cypher.anyNode("n").relationshipTo(node("S2").named("m"), "SOMEHOW_RELATED")).with("n", "m");

			Statement statement = step1Supplier.andThen(step2Supplier).apply(Statement.builder()).returning("n", "m").build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`S1`) WHERE n.a = 'A' WITH n MATCH (n)-[:`SOMEHOW_RELATED`]->(m:`S2`) WITH n, m RETURN n, m");
		}

		@Test
		void optionalNext() {
			Statement statement = Cypher
				.match(bikeNode)
				.optionalMatch(userNode, Cypher.node("U").named("o"))
				.where(userNode.property("a").isNull())
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) OPTIONAL MATCH (u:`User`), (o:`U`) WHERE u.a IS NULL RETURN b");
		}

		@Test
		void optionalMatchThenDelete() {
			Statement statement = Cypher
				.match(bikeNode)
				.optionalMatch(userNode, Cypher.node("U").named("o"))
				.delete(userNode, bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (b:`Bike`) OPTIONAL MATCH (u:`User`), (o:`U`) DELETE u, b");
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
				.returning(coalesce(userNode.property("a"), userNode.property("b"), literalOf("¯\\_(ツ)_/¯")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN coalesce(u.a, u.b, '¯\\\\_(ツ)_/¯')");
		}

		@Test
		void literalsShouldDealWithNull() {
			Statement statement = Cypher.match(userNode)
				.returning(Functions.coalesce(literalOf(null), userNode.property("field")).as("p"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) RETURN coalesce(NULL, u.field) AS p");
		}
	}

	@Nested
	class ComparisonRendering {

		@Test
		void equalsWithStringLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("name").isEqualTo(literalOf("Test")))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.name = 'Test' RETURN u");
		}

		@Test
		void equalsWithNumberLiteral() {
			Statement statement = Cypher.match(userNode)
				.where(userNode.property("age").isEqualTo(literalOf(21)))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.age = 21 RETURN u");
		}
	}

	@Nested
	class Conditions {
		@Test
		void conditionsChainingAnd() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(literalOf("Test"))
						.and(userNode.property("age").isEqualTo(literalOf(21))))
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
					userNode.property("name").isEqualTo(literalOf("Test"))
						.or(userNode.property("age").isEqualTo(literalOf(21))))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.name = 'Test' OR u.age = 21) RETURN u");
		}

		@Test
		void nestedConditions() {
			Statement statement;

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true) RETURN u");

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()))
				.or(isFalse())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (((true OR false) AND true) OR false) RETURN u");

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()))
				.or(isFalse())
				.and(isFalse())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((((true OR false) AND true) OR false) AND false) RETURN u");

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()))
				.or(isFalse().and(isTrue()))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false AND true)) RETURN u");

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()))
				.or(isFalse().and(isTrue()))
				.and(isTrue())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false AND true) AND true) RETURN u");

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()))
				.or(isFalse().or(isTrue()))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false OR true)) RETURN u");

			statement = Cypher.match(userNode)
				.where(isTrue().or(isFalse()).and(isTrue()).or(isFalse().or(isTrue())))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (u:`User`) WHERE ((true OR false) AND true OR (false OR true)) RETURN u");
		}

		@Test
		void conditionsChainingXor() {
			Statement statement = Cypher.match(userNode)
				.where(
					userNode.property("name").isEqualTo(literalOf("Test"))
						.xor(userNode.property("age").isEqualTo(literalOf(21))))
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
				.or(userNode.property("age").isEqualTo(literalOf(21)))
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
				.where(userNode.property("name").isEqualTo(literalOf("test")))
				.and(noCondition()).or(noCondition())
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.name = 'test' RETURN u");
		}
	}

	@Nested
	class RemoveClause {
		@Test
		void shouldRenderRemoveOnNodes() {
			Statement statement;

			statement = Cypher.match(userNode)
				.remove(userNode, "A", "B")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) REMOVE u:`A`:`B` RETURN u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.set(userNode, "A", "B")
				.remove(userNode, "C", "D")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u SET u:`A`:`B` REMOVE u:`C`:`D` RETURN u");
		}

		@Test
		void shouldRenderRemoveOfProperties() {
			Statement statement;

			statement = Cypher.match(userNode)
				.remove(userNode.property("a"), userNode.property("b"))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) REMOVE u.a, u.b RETURN u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.remove(userNode.property("a"))
				.remove(userNode.property("b"))
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u REMOVE u.a REMOVE u.b RETURN u");
		}
	}

	@Nested
	class SetClause {

		@Test
		void shouldRenderSetAfterCreate() {
			Statement statement;
			statement = Cypher.create(userNode)
				.set(userNode.property("p").to(literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSetAfterMerge() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.set(userNode.property("p").to(literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSetAfterCreateAndWith() {
			Statement statement;
			statement = Cypher.create(userNode)
				.with(userNode)
				.set(userNode.property("p").to(literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) WITH u SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSetAfterMergeAndWith() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.with(userNode)
				.set(userNode.property("p").to(literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) WITH u SET u.p = 'Hallo, Welt'");
		}

		@Test
		void shouldRenderSet() {

			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode.property("p").to(literalOf("Hallo, Welt")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt'");

			statement = Cypher.match(userNode)
				.set(userNode.property("p").to(literalOf("Hallo, Welt")))
				.set(userNode.property("a").to(literalOf("Selber hallo.")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt' SET u.a = 'Selber hallo.'");

			statement = Cypher.match(userNode)
				.set(
					userNode.property("p").to(literalOf("Hallo")),
					userNode.property("g").to(literalOf("Welt"))
				)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo', u.g = 'Welt'");

		}

		@Test
		void shouldRenderSetOnNodes() {
			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode, "A", "B")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u:`A`:`B` RETURN u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.set(userNode, "A", "B")
				.set(userNode, "C", "D")
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u SET u:`A`:`B` SET u:`C`:`D` RETURN u");
		}

		@Test
		void shouldRenderSetFromAListOfExpression() {
			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode.property("p"), literalOf("Hallo, Welt"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt'");

			statement = Cypher.match(userNode)
				.set(userNode.property("p"), literalOf("Hallo"),
					userNode.property("g"), literalOf("Welt"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo', u.g = 'Welt'");

			statement = Cypher.match(userNode)
				.set(userNode.property("p"), literalOf("Hallo, Welt"))
				.set(userNode.property("p"), literalOf("Hallo"),
					userNode.property("g"), literalOf("Welt"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p = 'Hallo, Welt' SET u.p = 'Hallo', u.g = 'Welt'");

			assertThatIllegalArgumentException().isThrownBy(() -> {
				Cypher.match(userNode).set(userNode.property("g"));
			}).withMessage("The list of expression to set must be even.");
		}

		@Test
		void shouldRenderMixedSet() {
			Statement statement;

			statement = Cypher.match(userNode)
				.set(userNode.property("p1"), literalOf("Two expressions"))
				.set(userNode.property("p2").to(literalOf("A set expression")))
				.set(
					userNode.property("p3").to(literalOf("One of two set expression")),
					userNode.property("p4").to(literalOf("Two of two set expression"))
				)
				.set(
					userNode.property("p5"), literalOf("Pair one of 2 expressions"),
					userNode.property("p6"), literalOf("Pair two of 4 expressions")
				)
				.returning(asterisk())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) SET u.p1 = 'Two expressions' SET u.p2 = 'A set expression' SET u.p3 = 'One of two set expression', u.p4 = 'Two of two set expression' SET u.p5 = 'Pair one of 2 expressions', u.p6 = 'Pair two of 4 expressions' RETURN *");
		}
	}

	@Nested
	class MergeClause {

		@Test
		void shouldRenderMergeWithoutReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)");

			statement = Cypher.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void shouldRenderMultipleMergesWithoutReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.merge(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) MERGE (b:`Bike`)");

			statement = Cypher
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.merge(Cypher.node("Other"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`) MERGE (:`Other`)");
		}

		@Test
		void shouldRenderMergeReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.merge(r)
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN u, o");

			statement = Cypher.merge(userNode)
				.returning(userNode)
				.orderBy(userNode.property("name"))
				.skip(23)
				.limit(42)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) RETURN u ORDER BY u.name SKIP 23 LIMIT 42");
		}

		@Test
		void shouldRenderMultipleMergesReturn() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.merge(bikeNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) MERGE (b:`Bike`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.merge(Cypher.node("Other"))
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`)-[o:`OWNS`]->(b:`Bike`) MERGE (:`Other`) RETURN u, o");
		}

		@Test
		void shouldRenderMergeWithWith() {
			Statement statement;
			statement = Cypher.merge(userNode)
				.with(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) WITH u RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.merge(userNode)
				.with(userNode)
				.set(userNode.property("x").to(literalOf("y")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MERGE (u:`User`) WITH u SET u.x = 'y'");
		}

		@Test
		void matchShouldExposeMerge() {
			Statement statement;
			statement = Cypher.match(userNode)
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) MERGE (u)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void withShouldExposeMerge() {
			Statement statement;
			statement = Cypher.match(userNode)
				.withDistinct(userNode)
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH DISTINCT u MERGE (u)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void mixedCreateAndMerge() {
			Statement statement;

			Node tripNode = Cypher.node("Trip").named("t");

			statement = Cypher.create(userNode)
				.merge(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.withDistinct(bikeNode)
				.merge(tripNode.relationshipFrom(bikeNode, "USED_ON"))
				.returning(Cypher.asterisk())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) MERGE (u)-[o:`OWNS`]->(b:`Bike`) WITH DISTINCT b MERGE (t:`Trip`)<-[:`USED_ON`]-(b) RETURN *");
		}
	}

	@Nested
	class CreateClause {

		@Test
		void shouldRenderCreateWithoutReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)");

			statement = Cypher.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void shouldRenderMultipleCreatesWithoutReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.create(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) CREATE (b:`Bike`)");

			statement = Cypher
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.create(Cypher.node("Other"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`) CREATE (:`Other`)");
		}

		@Test
		void shouldRenderCreateReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.create(r)
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`) RETURN u, o");

			statement = Cypher.create(userNode)
				.returning(userNode)
				.orderBy(userNode.property("name"))
				.skip(23)
				.limit(42)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) RETURN u ORDER BY u.name SKIP 23 LIMIT 42");
		}

		@Test
		void shouldRenderMultipleCreatesReturn() {
			Statement statement;
			statement = Cypher.create(userNode)
				.create(bikeNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) CREATE (b:`Bike`) RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.create(Cypher.node("Other"))
				.returning(userNode, r)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`)-[o:`OWNS`]->(b:`Bike`) CREATE (:`Other`) RETURN u, o");
		}

		@Test
		void shouldRenderCreateWithWith() {
			Statement statement;
			statement = Cypher.create(userNode)
				.with(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) WITH u RETURN u");

			Relationship r = userNode.relationshipTo(bikeNode, "OWNS").named("o");
			statement = Cypher.create(userNode)
				.with(userNode)
				.set(userNode.property("x").to(literalOf("y")))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"CREATE (u:`User`) WITH u SET u.x = 'y'");
		}

		@Test
		void matchShouldExposeCreate() {
			Statement statement;
			statement = Cypher.match(userNode)
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) CREATE (u)-[o:`OWNS`]->(b:`Bike`)");
		}

		@Test
		void withShouldExposeCreate() {
			Statement statement;
			statement = Cypher.match(userNode)
				.withDistinct(userNode)
				.create(userNode.relationshipTo(bikeNode, "OWNS").named("o"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH DISTINCT u CREATE (u)-[o:`OWNS`]->(b:`Bike`)");
		}
	}

	@Nested
	class DeleteClause {

		@Test
		void shouldRenderDeleteWithoutReturn() {

			Statement statement;
			statement = Cypher.match(userNode)
				.detachDelete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) DETACH DELETE u");

			statement = Cypher.match(userNode)
				.with(userNode)
				.detachDelete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WITH u DETACH DELETE u");

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
				.detachDelete(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) DETACH DELETE u RETURN u");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.detachDelete(userNode)
				.returning(userNode).orderBy(userNode.property("a").ascending()).skip(2).limit(1)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DETACH DELETE u RETURN u ORDER BY u.a ASC SKIP 2 LIMIT 1");

			statement = Cypher.match(userNode)
				.where(userNode.property("a").isNotNull()).and(userNode.property("b").isNull())
				.detachDelete(userNode)
				.returningDistinct(userNode).orderBy(userNode.property("a").ascending()).skip(2).limit(1)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE (u.a IS NOT NULL AND u.b IS NULL) DETACH DELETE u RETURN DISTINCT u ORDER BY u.a ASC SKIP 2 LIMIT 1");
		}

		@Test
		void shouldRenderNodeDelete() {
			Node n = anyNode("n");
			Relationship r = n.relationshipBetween(anyNode()).named("r0");
			Statement statement = Cypher
				.match(n).where(n.internalId().isEqualTo(literalOf(4711)))
				.optionalMatch(r)
				.delete(r, n)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) WHERE id(n) = 4711 OPTIONAL MATCH (n)-[r0]-() DELETE r0, n");
		}

		@Test
		void shouldRenderChainedDeletes() {
			Node n = anyNode("n");
			Relationship r = n.relationshipBetween(anyNode()).named("r0");
			Statement statement = Cypher
				.match(n).where(n.internalId().isEqualTo(literalOf(4711)))
				.optionalMatch(r)
				.delete(r, n)
				.delete(bikeNode)
				.detachDelete(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) WHERE id(n) = 4711 OPTIONAL MATCH (n)-[r0]-() DELETE r0, n DELETE b DETACH DELETE u");
		}
	}

	@Nested
	class Expressions {
		@Test
		void shouldRenderParameters() {
			Statement statement;
			statement = Cypher.match(userNode)
				.where(userNode.property("a").isEqualTo(parameter("aParameter")))
				.detachDelete(userNode)
				.returning(userNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (u:`User`) WHERE u.a = $aParameter DETACH DELETE u RETURN u");
		}
	}

	@Nested
	class OperationsAndComparisons {

		@Test
		void shouldRenderOperations() {
			Statement statement;
			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(literalOf(1).plus(literalOf(2)))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN (1 + 2)");
		}

		@Test
		void shouldRenderComparision() {
			Statement statement;
			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(literalOf(1).gt(literalOf(2)))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN 1 > 2");

			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(literalOf(1).gt(literalOf(2)).isTrue())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN (1 > 2) = true");

			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(literalOf(1).gt(literalOf(2)).isTrue().isFalse())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN ((1 > 2) = true) = false");
		}
	}

	@Nested
	class ExpressionsRendering {
		@Test
		void shouldRenderMap() {
			Statement statement;
			statement = Cypher.match(Cypher.anyNode("n"))
				.returning(
					Functions.point(
						mapOf(
							"latitude", Cypher.parameter("latitude"),
							"longitude", Cypher.parameter("longitude"),
							"crs", literalOf(4326)
						)
					)
				)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN point({latitude: $latitude, longitude: $longitude, crs: 4326})");
		}
	}

	@Nested
	class PropertyRendering {
		@Test
		void shouldRenderNodeProperties() {

			for (Node nodeWithProperties : new Node[] {
				Cypher.node("Test", mapOf("a", literalOf("b"))),
				Cypher.node("Test").properties(mapOf("a", literalOf("b"))),
				Cypher.node("Test").properties("a", literalOf("b"))
			}) {

				Statement statement;
				statement = Cypher.match(nodeWithProperties)
					.returning(Cypher.asterisk())
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (:`Test` {a: 'b'}) RETURN *");

				statement = Cypher.merge(nodeWithProperties)
					.returning(Cypher.asterisk())
					.build();

				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MERGE (:`Test` {a: 'b'}) RETURN *");
			}
		}

		@Test
		void nestedProperties() {

			Node nodeWithProperties = Cypher.node("Test").properties("outer", mapOf("a", literalOf("b")));

			Statement statement;
			statement = Cypher.match(nodeWithProperties)
				.returning(Cypher.asterisk())
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (:`Test` {outer: {a: 'b'}}) RETURN *");
		}

		@Test
		void shouldNotRenderPropertiesInReturn() {

			Node nodeWithProperties = bikeNode.properties("a", literalOf("b"));

			Statement statement;
			statement = Cypher.match(nodeWithProperties, nodeWithProperties.relationshipFrom(userNode, "OWNS"))
				.returning(nodeWithProperties)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike` {a: 'b'}), (b)<-[:`OWNS`]-(u:`User`) RETURN b");
		}
	}

	@Nested
	class UnwindRendering {

		@Test
		void shouldRenderLeadingUnwind() {

			Statement statement;
			statement = Cypher.unwind(Cypher.literalOf(1), Cypher.literalTrue(), Cypher.literalFalse())
				.as("n").returning(name("n"))
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"UNWIND [1, true, false] AS n RETURN n");
		}

		@Test
		void shouldRenderLeadingUnwindWithUpdate() {

			Statement statement;
			statement = Cypher.unwind(Cypher.literalOf(1), Cypher.literalTrue(), Cypher.literalFalse())
				.as("n")
				.merge(bikeNode.properties("b", name("n")))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"UNWIND [1, true, false] AS n MERGE (b:`Bike` {b: n}) RETURN b");
		}

		@Test
		void shouldRenderLeadingUnwindWithCreate() {

			Statement statement;
			statement = Cypher.unwind(Cypher.literalOf(1), Cypher.literalTrue(), Cypher.literalFalse())
				.as("n")
				.create(bikeNode.properties("b", name("n")))
				.returning(bikeNode)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"UNWIND [1, true, false] AS n CREATE (b:`Bike` {b: n}) RETURN b");
		}

		@Test
		void shouldRenderUnwind() {

			Statement statement;

			AliasedExpression collected = collect(bikeNode).as("collected");
			statement = Cypher.match(bikeNode)
				.with(collected)
				.unwind(collected).as("x")
				.with("x")
				.delete(name("x"))
				.returning("x")
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WITH collect(b) AS collected UNWIND collected AS x WITH x DELETE x RETURN x");
		}
	}

	@Nested
	class Unions {

		@Test
		void shouldRenderUnions() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement3 = Cypher.match(bikeNode)
				.where(bikeNode.property("c").isEqualTo(literalOf("C")))
				.returning(bikeNode)
				.build();
			Statement statement;
			statement = Cypher.union(statement1, statement2, statement3);

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a = 'A' RETURN b UNION MATCH (b) WHERE b.b = 'B' RETURN b UNION MATCH (b) WHERE b.c = 'C' RETURN b");
		}

		@Test
		void shouldRenderAllUnions() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement;
			statement = Cypher.unionAll(statement1, statement2);

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a = 'A' RETURN b UNION ALL MATCH (b) WHERE b.b = 'B' RETURN b");
		}

		@Test
		void shouldAppendToExistingUnions() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement;
			statement = Cypher.unionAll(statement1, statement2);

			Statement statement3 = Cypher.match(bikeNode)
				.where(bikeNode.property("c").isEqualTo(literalOf("C")))
				.returning(bikeNode)
				.build();

			Statement statement4 = Cypher.match(bikeNode)
				.where(bikeNode.property("d").isEqualTo(literalOf("D")))
				.returning(bikeNode)
				.build();

			statement = Cypher.unionAll(statement, statement3, statement4);

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (b:`Bike`) WHERE b.a = 'A' RETURN b UNION ALL MATCH (b) WHERE b.b = 'B' RETURN b UNION ALL MATCH (b) WHERE b.c = 'C' RETURN b UNION ALL MATCH (b) WHERE b.d = 'D' RETURN b");
		}

		@Test
		void shouldNotMix() {

			Statement statement1 = Cypher.match(bikeNode)
				.where(bikeNode.property("a").isEqualTo(literalOf("A")))
				.returning(bikeNode)
				.build();

			Statement statement2 = Cypher.match(bikeNode)
				.where(bikeNode.property("b").isEqualTo(literalOf("B")))
				.returning(bikeNode)
				.build();

			Statement statement;
			statement = Cypher.unionAll(statement1, statement2);

			Statement statement3 = Cypher.match(bikeNode)
				.where(bikeNode.property("c").isEqualTo(literalOf("C")))
				.returning(bikeNode)
				.build();

			assertThatIllegalArgumentException().isThrownBy(() ->
				Cypher.union(statement, statement3)).withMessage("Cannot mix union and union all!");

		}
	}

	@Nested
	class MapProjections {

		@Nested
		class OnNodes {

			@Test
			void simple() {

				Statement statement;
				Node n = anyNode("n");

				statement = Cypher.match(n)
					.returning(n.project("__internalNeo4jId__", Functions.id(n), "name"))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (n) RETURN n{__internalNeo4jId__: id(n), .name}");

				statement = Cypher.match(n)
					.returning(n.project("name", "__internalNeo4jId__", Functions.id(n)))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (n) RETURN n{.name, __internalNeo4jId__: id(n)}");
			}

			@Test
			void nested() {

				Statement statement;
				Node n = Cypher.node("Person").named("p");
				Node m = Cypher.node("Movie").named("m");

				statement = Cypher.match(n.relationshipTo(m, "ACTED_IN"))
					.returning(
						n.project(
							"__internalNeo4jId__", Functions.id(n), "name", "nested",
							m.project("title", "__internalNeo4jId__", Functions.id(m))
						))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (p:`Person`)-[:`ACTED_IN`]->(m:`Movie`) RETURN p{__internalNeo4jId__: id(p), .name, nested: m{.title, __internalNeo4jId__: id(m)}}");
			}

			@Test
			void requiresSymbolicName() {
				assertThatIllegalStateException().isThrownBy(() -> {
					Node n = Cypher.node("Person");
					n.project("something");
				}).withMessage("No name present.");
			}
		}

		@Nested
		class OnRelationShips {

			@Test
			void simple() {

				Statement statement;
				Node n = Cypher.node("Person").named("p");
				Node m = Cypher.node("Movie").named("m");
				Relationship rel = n.relationshipTo(m, "ACTED_IN").named("r");

				statement = Cypher.match(rel)
					.returning(
						rel.project(
							"__internalNeo4jId__", Functions.id(rel), "roles"
						))
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (p:`Person`)-[r:`ACTED_IN`]->(m:`Movie`) RETURN r{__internalNeo4jId__: id(r), .roles}");
			}

			@Test
			void nested() {

				Statement statement;
				Node n = Cypher.node("Person").named("p");
				Node m = Cypher.node("Movie").named("m");
				Relationship rel = n.relationshipTo(m, "ACTED_IN").named("r");

				statement = Cypher.match(rel)
					.returning(
						m.project(
							"title", "roles",
							rel.project(
								"__internalNeo4jId__", Functions.id(rel), "roles"
							)
						)
					)
					.build();
				assertThat(cypherRenderer.render(statement))
					.isEqualTo(
						"MATCH (p:`Person`)-[r:`ACTED_IN`]->(m:`Movie`) RETURN m{.title, roles: r{__internalNeo4jId__: id(r), .roles}}");
			}

			@Test
			void requiresSymbolicName() {
				assertThatIllegalStateException().isThrownBy(() -> {
					Node n = Cypher.node("Person").named("p");
					Node m = Cypher.node("Movie").named("m");
					Relationship rel = n.relationshipTo(m, "ACTED_IN");
					rel.project("something");
				}).withMessage("No name present.");
			}
		}

		@Test
		void asterisk() {

			Statement statement;
			Node n = anyNode("n");

			statement = Cypher.match(n)
				.returning(n.project(Cypher.asterisk()))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n) RETURN n{.*}");
		}

		@Test
		void invalid() {

			String expectedMessage = "FunctionInvocation{functionName='id'} of type class org.neo4j.springframework.data.core.cypher.FunctionInvocation cannot be used with an implicit name as map entry.";
			assertThatIllegalArgumentException().isThrownBy(() -> {
				Node n = anyNode("n");
				n.project(Functions.id(n));
			}).withMessage(expectedMessage);

			assertThatIllegalArgumentException().isThrownBy(() -> {
				Node n = anyNode("n");
				n.project("a", Cypher.mapOf("a", Cypher.literalOf("b")), Functions.id(n));
			}).withMessage(expectedMessage);
		}
	}

	@Nested
	class WithAndOrder {

		@Test
		void orderOnWithShouldWork() {
			Statement statement = Cypher
				.match(
					node("Movie").named("m").relationshipFrom(node("Person").named("p"), "ACTED_IN").named("r")
				)
				.with(name("m"), name("p"))
				.orderBy(
					sort(property("m", "title")),
					sort(property("p", "name"))
				).returning(property("m", "title").as("movie"),
					collect(property("p", "name")).as("actors")).build();

			String expected = "MATCH (m:`Movie`)<-[r:`ACTED_IN`]-(p:`Person`) WITH m, p ORDER BY m.title, p.name RETURN m.title AS movie, collect(p.name) AS actors";
			assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
		}

		@Test
		void concatenatedOrdering() {
			Statement statement;
			statement = Cypher.match(
				node("Movie").named("m").relationshipFrom(node("Person").named("p"), "ACTED_IN").named("r"))
				.with(name("m"), name("p")).orderBy(property("m", "title")).ascending()
				.and(property("p", "name")).ascending()
				.returning(property("m", "title").as("movie"),
					collect(property("p", "name")).as("actors")).build();

			String expected = "MATCH (m:`Movie`)<-[r:`ACTED_IN`]-(p:`Person`) WITH m, p ORDER BY m.title ASC, p.name ASC RETURN m.title AS movie, collect(p.name) AS actors";
			assertThat(cypherRenderer.render(statement)).isEqualTo(expected);
		}
	}

	@Nested
	class PatternComprehensions {

		@Test
		void simple() {

			Statement statement;
			Node a = Cypher.node("Person").properties("name", literalOf("Keanu Reeves")).named("a");
			Node b = Cypher.anyNode("b");

			statement = Cypher.match(a)
				.returning(listBasedOn(a.relationshipBetween(b)).returning(b.property("released")).as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b)|b.released] AS years");
		}

		@Test
		void simpleWithWhere() {

			Statement statement;
			Node a = Cypher.node("Person").properties("name", literalOf("Keanu Reeves")).named("a");
			Node b = Cypher.anyNode("b");

			statement = Cypher.match(a)
				.returning(
					listBasedOn(a.relationshipBetween(b)).where(b.hasLabels("Movie")).returning(b.property("released"))
						.as("years"))
				.build();
			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (a:`Person` {name: 'Keanu Reeves'}) RETURN [(a)--(b) WHERE b:`Movie`|b.released] AS years");
		}

		@Test
		void nested() {

			Statement statement;

			Node n = Cypher.node("Person").named("n");
			Node o1 = Cypher.node("Organisation").named("o1");
			Node l1 = Cypher.node("Location").named("l1");
			Node p2 = Cypher.node("Person").named("p2");

			Relationship r_f1 = n.relationshipTo(o1, "FOUNDED").named("r_f1");
			Relationship r_e1 = n.relationshipTo(o1, "EMPLOYED_BY").named("r_e1");
			Relationship r_l1 = n.relationshipTo(l1, "LIVES_AT").named("r_l1");
			Relationship r_l2 = l1.relationshipFrom(p2, "LIVES_AT").named("r_l2");

			statement = Cypher.match(n)
				.returning(n.getRequiredSymbolicName(),
					listOf(
						listBasedOn(r_f1).returning(r_f1, o1),
						listBasedOn(r_e1).returning(r_e1, o1),
						listBasedOn(r_l1).returning(
							r_l1.getRequiredSymbolicName(), l1.getRequiredSymbolicName(),
							// The building of the statement works with and without the outer list,
							// I'm not sure if it would be necessary for the result, but as I took the query from
							// Neo4j-OGM, I'd like to keep it
							listOf(listBasedOn(r_l2).returning(r_l2, p2))
						)
					)
				)
				.build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (n:`Person`) RETURN n, [[(n)-[r_f1:`FOUNDED`]->(o1:`Organisation`)|[r_f1, o1]], [(n)-[r_e1:`EMPLOYED_BY`]->(o1)|[r_e1, o1]], [(n)-[r_l1:`LIVES_AT`]->(l1:`Location`)|[r_l1, l1, [[(l1)<-[r_l2:`LIVES_AT`]-(p2:`Person`)|[r_l2, p2]]]]]]");
		}
	}

	@Nested
	class MultipleLabels {

		@Test
		void matchWithMultipleLabels() {
			Node node = node("a", "b", "c").named("n");
			Statement statement = match(node).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`:`b`:`c`) RETURN n");
		}

		@Test
		void createWithMultipleLabels() {
			Node node = node("a", "b", "c").named("n");
			Statement statement = create(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("CREATE (n:`a`:`b`:`c`)");
		}
	}

	@Nested
	class Case {

		@Test
		void simpleCase() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(literalOf("blubb"))
					.then(literalTrue())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true END RETURN n");
		}

		@Test
		void simpleCaseWithElse() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(literalOf("blubb"))
					.then(literalTrue())
					.elseDefault(literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true ELSE false END RETURN n");
		}

		@Test
		void simpleCaseWithMultipleWhenThen() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(literalOf("blubb"))
					.then(literalTrue())
					.when(literalOf("bla"))
					.then(literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true WHEN 'bla' THEN false END RETURN n");
		}

		@Test
		void simpleCaseWithMultipleWhenThenAndElse() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression(node.property("value"))
					.when(literalOf("blubb"))
					.then(literalTrue())
					.when(literalOf("bla"))
					.then(literalFalse())
					.elseDefault(literalOf(1))
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE n.value WHEN 'blubb' THEN true WHEN 'bla' THEN false ELSE 1 END RETURN n");
		}

		@Test
		void genericCase() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(literalOf("blubb")))
					.then(literalTrue())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true END RETURN n");
		}

		@Test
		void genericCaseWithElse() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(literalOf("blubb")))
					.then(literalTrue())
					.elseDefault(literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true ELSE false END RETURN n");
		}

		@Test
		void genericCaseWithMultipleWhenThen() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(literalOf("blubb")))
					.then(literalTrue())
					.when(node.property("value").isEqualTo(literalOf("bla")))
					.then(literalFalse())
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true WHEN n.value = 'bla' THEN false END RETURN n");
		}

		@Test
		void genericCaseWithMultipleWhenThenAndElse() {
			Node node = node("a").named("n");
			Statement statement = match(node).where(
				Cypher.caseExpression()
					.when(node.property("value").isEqualTo(literalOf("blubb")))
					.then(literalTrue())
					.when(node.property("value").isEqualTo(literalOf("bla")))
					.then(literalFalse())
					.elseDefault(literalOf(1))
			).returning(node).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n:`a`) WHERE CASE WHEN n.value = 'blubb' THEN true WHEN n.value = 'bla' THEN false ELSE 1 END RETURN n");
		}

		// from https://neo4j.com/docs/cypher-manual/current/syntax/expressions/#syntax-simple-case
		@Test
		void canGetAliasedInReturn() {
			Node node = anyNode("n");
			Statement statement = match(node)
				.returning(
					Cypher.caseExpression(node.property("eyes"))
						.when(literalOf("blue"))
						.then(literalOf(1))
						.when(literalOf("brown"))
						.then(literalOf(2))
						.elseDefault(literalOf(3))
						.as("result")
				).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo("MATCH (n) RETURN CASE n.eyes WHEN 'blue' THEN 1 WHEN 'brown' THEN 2 ELSE 3 END AS result");
		}
	}

	@Nested
	class Issues {

		@Test
		void gh167() {
			final Node app = node("Location").named("app").properties("uuid", parameter("app_uuid"));
			final Node locStart = node("Location").named("loc_start");
			final Node resume = node("Resume").named("r");
			final Node offer = node("Offer").named("o");
			final Node startN = node("ResumeNode").named("start_n");

			final Relationship aFl = app.relationshipFrom(locStart, "PART_OF").length(0, 3);
			final Relationship lFr = locStart.relationshipFrom(resume, "IN", "IN_ANALYTICS");

			Statement statement = match(aFl, lFr)
				.withDistinct(resume, locStart, app)
				.match(resume
					.relationshipTo(offer.properties("is_valid", literalTrue()), "IN_COHORT_OF")
					.relationshipTo(anyNode("app"), "IN")
				)
				.withDistinct(resume, locStart, app, offer)
				.match(offer.relationshipTo(startN, "FOR"))
				.where(Functions.id(startN).in(parameter("start_ids")))
				.returningDistinct(resume, locStart, app, offer, startN).build();

			assertThat(cypherRenderer.render(statement))
				.isEqualTo(
					"MATCH (app:`Location` {uuid: $app_uuid})<-[:`PART_OF`*0..3]-(loc_start:`Location`), (loc_start)<-[:`IN`|`IN_ANALYTICS`]-(r:`Resume`) WITH DISTINCT r, loc_start, app MATCH (r)-[:`IN_COHORT_OF`]->(o:`Offer` {is_valid: true})-[:`IN`]->(app) WITH DISTINCT r, loc_start, app, o MATCH (o:`Offer`)-[:`FOR`]->(start_n:`ResumeNode`) WHERE id(start_n) IN $start_ids RETURN DISTINCT r, loc_start, app, o, start_n");
		}

		@Test
		void gh174() {
			final Node r = node("Resume").named("r");
			final Node o = node("Offer").named("o");

			Statement s = match(r.relationshipTo(o, "FOR"))
				.where(r.hasLabels("LastResume").not())
				.and(coalesce(o.property("valid_only"), literalFalse()).isEqualTo(literalFalse())
					.and(r.hasLabels("InvalidStatus").not())
					.or(o.property("valid_only").isTrue()
						.and(r.hasLabels("InvalidStatus"))))
				.returningDistinct(r, o)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)-[:`FOR`]->(o:`Offer`) WHERE (NOT (r:`LastResume`) AND (coalesce(o.valid_only, false) = false AND NOT (r:`InvalidStatus`) OR (o.valid_only = true AND r:`InvalidStatus`))) RETURN DISTINCT r, o");
		}

		@Test
		void gh184() {
			final Node r = node("Resume").named("r");
			final Node u = node("UserSearchable").named("u");
			final Node o = node("Offer").named("o");

			Statement s = match(r.relationshipFrom(u, "HAS"))
				.where(r.hasLabels("LastResume").not())
				.and(coalesce(o.property("valid_only"), literalFalse()).isEqualTo(literalFalse())
					.and(r.hasLabels("InvalidStatus").not())
					.or(o.property("valid_only").isTrue()
						.and(r.hasLabels("ValidStatus")))
				)
				.and(r.property("is_internship").isTrue()
					.and(size(r.relationshipTo(Cypher.anyNode(), "PART_OF")).isEmpty())
					.not())
				.and(r.property("is_sandwich_training").isTrue()
					.and(size(r.relationshipTo(Cypher.anyNode(), "PART_OF")).isEmpty())
					.not())
				.returningDistinct(r, o)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`UserSearchable`) "
					+ "WHERE (NOT (r:`LastResume`) "
					+   "AND (coalesce(o.valid_only, false) = false "
					+   "AND NOT (r:`InvalidStatus`) "
					+   "OR (o.valid_only = true "
					+     "AND r:`ValidStatus`)) "
					+   "AND NOT ("
					+     "(r.is_internship = true AND size(size((r)-[:`PART_OF`]->())) = 0)"
					+   ") "
					+   "AND NOT ("
					+     "(r.is_sandwich_training = true AND size(size((r)-[:`PART_OF`]->())) = 0)"
					+   ")"
					+ ") RETURN DISTINCT r, o");
		}

		@Test
		void gh185() {
			final Node r = node("Resume").named("r");
			final Node u = node("UserSearchable").named("u");

			Statement s = match(r.relationshipFrom(u, "HAS"))
				.where(not(exists(r.relationshipTo(u, "EXCLUDES"))))
				.returningDistinct(r)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`UserSearchable`) WHERE NOT (exists((r)-[:`EXCLUDES`]->(u))) RETURN DISTINCT r");
		}

		@Test
		void gh187() {
			final Node r = node("Resume").named("r");
			final Node u = node("User").named("u");

			Statement s = match(r.relationshipFrom(u, "HAS"))
				.with(Functions.head(Functions.collect(r.getRequiredSymbolicName())).as("r"))
					.returning(r)
					.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`User`) WITH head(collect(r)) AS r RETURN r");
		}

		@Test
		void gh188() {
			final Node r = node("Resume").named("r");
			final Node u = node("User").named("u");

			Statement s = match(r.relationshipFrom(u, "HAS"))
				.returning(countDistinct(r.getRequiredSymbolicName()).as("r"))
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`)<-[:`HAS`]-(u:`User`) RETURN count(DISTINCT r) AS r");
		}

		@Test
		void gh197() {
			final Node n = node("Person").named("n");

			// avg
			Statement s = match(n)
				.returning(avg(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) RETURN avg(n.age)");

			// max/min
			final ListExpression list = listOf(
				literalOf(1),
				literalOf("a"),
				literalOf(null),
				literalOf(0.2),
				literalOf("b"),
				literalOf("1"),
				literalOf("99"));
			s = unwind(list).as("val")
				.returning(max(name("val"))).build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("UNWIND [1, 'a', NULL, 0.2, 'b', '1', '99'] AS val RETURN max(val)");
			s = unwind(list).as("val")
				.returning(min(name("val"))).build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("UNWIND [1, 'a', NULL, 0.2, 'b', '1', '99'] AS val RETURN min(val)");

			// percentileCont/percentileDisc
			s = match(n)
				.returning(percentileCont(n.property("age"), 0.4))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) RETURN percentileCont(n.age, 0.4)");
			s = match(n)
				.returning(percentileDisc(n.property("age"), 0.5))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) RETURN percentileDisc(n.age, 0.5)");

			// stDev/stDevP
			s = match(n)
				.where(n.property("name").in(listOf(literalOf("A"), literalOf("B"), literalOf("C"))))
				.returning(stDev(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) WHERE n.name IN ['A', 'B', 'C'] RETURN stDev(n.age)");
			s = match(n)
				.where(n.property("name").in(listOf(literalOf("A"), literalOf("B"), literalOf("C"))))
				.returning(stDevP(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) WHERE n.name IN ['A', 'B', 'C'] RETURN stDevP(n.age)");

			// sum
			s = match(n)
				.with(listOf(mapOf(
					"type", n.getRequiredSymbolicName(),
					"nb", sum(n.getRequiredSymbolicName())))
					.as("counts"))
				.returning(sum(n.property("age")))
				.build();
			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (n:`Person`) WITH [{type: n, nb: sum(n)}] AS counts RETURN sum(n.age)");
		}

		@Test
		void gh200() {
			final Node r = node("Resume").named("r");

			Statement s = match(r)
				.with(r.getRequiredSymbolicName())
				.returningDistinct(r.getRequiredSymbolicName())
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (r:`Resume`) WITH r RETURN DISTINCT r");
		}


		@Test
		void gh204() {
			final Node a = node("A").named("a");
			final Node b = node("B").named("b");
			final Node c = node("C").named("c");

			Statement s = match(a.relationshipTo(b).relationshipTo(c).max(2))
				.returning(a)
				.build();

			assertThat(cypherRenderer.render(s))
				.isEqualTo("MATCH (a:`A`)-->(b:`B`)-[*..2]->(c:`C`) RETURN a");
		}
	}
}
